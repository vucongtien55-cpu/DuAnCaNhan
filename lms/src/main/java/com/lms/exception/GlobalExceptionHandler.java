package com.lms.exception;

import com.lms.exception.ApiError.FieldIssue;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Comparator;
import java.util.List;

/**
 * Chuyển mọi ngoại lệ thành {@link ApiError} thống nhất. Thông điệp trả về luôn an toàn: chi tiết nội bộ (SQL, stack
 * trace, đường dẫn máy chủ) chỉ ghi vào log phía server.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ---------- ngoại lệ nghiệp vụ ----------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), request);
    }

    @ExceptionHandler(AttemptExpiredException.class)
    public ResponseEntity<ApiError> handleAttemptExpired(AttemptExpiredException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "ATTEMPT_EXPIRED", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", ex.getMessage(), request);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiError> handleStorage(StorageException ex, HttpServletRequest request) {
        log.error("Lỗi lưu trữ file tại {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR",
                "Không thể xử lý tệp, vui lòng thử lại sau", request);
    }

    // ---------- bảo mật ----------

    /** Thông điệp của BadCredentialsException do chính ứng dụng đặt và luôn chung chung (không lộ tài khoản tồn tại). */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Bạn chưa đăng nhập hoặc phiên đăng nhập không còn hiệu lực", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này", request);
    }

    // ---------- dữ liệu đầu vào và cơ sở dữ liệu ----------

    /** Lỗi kiểm tra đầu vào ở tầng service (@Validated) hoặc tham số Controller. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest request) {
        List<FieldIssue> details = ex.getConstraintViolations().stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .map(this::toIssue)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu không hợp lệ", request, details);
    }

    /** Vi phạm ràng buộc unique/khóa ngoại do hai yêu cầu đồng thời; không lộ câu lệnh SQL. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {
        log.warn("Vi phạm ràng buộc dữ liệu tại {}", request.getRequestURI(), ex);
        return build(HttpStatus.CONFLICT, "CONFLICT",
                "Dữ liệu xung đột với bản ghi hiện có, vui lòng tải lại và thử lại", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Lỗi không mong đợi tại {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau", request);
    }

    // ---------- ngoại lệ chuẩn của Spring MVC (sai kiểu dữ liệu, thiếu tham số, sai phương thức, ...) ----------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldIssue> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldIssue(error.getField(), error.getDefaultMessage()))
                .toList();
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu không hợp lệ", request, details, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<FieldIssue> details = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldIssue(
                                result.getMethodParameter().getParameterName() == null
                                        ? "" : result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu không hợp lệ", request, details, headers);
    }

    /** Mọi ngoại lệ MVC còn lại dùng thông điệp cố định theo mã trạng thái, không phản chiếu nội dung lỗi gốc. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        if (status.is5xxServerError()) {
            log.error("Lỗi máy chủ tại {}", pathOf(request), ex);
        }
        return body(status, codeOf(status), messageOf(status), request, List.of(), headers);
    }

    // ---------- helpers ----------

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                           HttpServletRequest request) {
        return build(status, code, message, request, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                           HttpServletRequest request, List<FieldIssue> details) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), code, message, request.getRequestURI(), details));
    }

    private ResponseEntity<Object> body(HttpStatus status, String code, String message, WebRequest request,
                                        List<FieldIssue> details, HttpHeaders headers) {
        return new ResponseEntity<>(ApiError.of(status.value(), code, message, pathOf(request), details),
                headers, status);
    }

    private FieldIssue toIssue(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        // "createUser.request.email" -> "email": bỏ tên phương thức và tham số, chỉ giữ tên trường
        String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        return new FieldIssue(field, violation.getMessage());
    }

    private String pathOf(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI() : "";
    }

    private String codeOf(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "BAD_REQUEST";
            case NOT_FOUND -> "NOT_FOUND";
            case METHOD_NOT_ALLOWED -> "METHOD_NOT_ALLOWED";
            case NOT_ACCEPTABLE -> "NOT_ACCEPTABLE";
            case UNSUPPORTED_MEDIA_TYPE -> "UNSUPPORTED_MEDIA_TYPE";
            case CONTENT_TOO_LARGE -> "PAYLOAD_TOO_LARGE";
            default -> status.is5xxServerError() ? "INTERNAL_ERROR" : "BAD_REQUEST";
        };
    }

    private String messageOf(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Không tìm thấy tài nguyên yêu cầu";
            case METHOD_NOT_ALLOWED -> "Phương thức HTTP không được hỗ trợ";
            case NOT_ACCEPTABLE -> "Định dạng phản hồi yêu cầu không được hỗ trợ";
            case UNSUPPORTED_MEDIA_TYPE -> "Định dạng dữ liệu gửi lên không được hỗ trợ";
            case CONTENT_TOO_LARGE -> "Dữ liệu gửi lên vượt quá dung lượng cho phép";
            default -> status.is5xxServerError()
                    ? "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau" : "Yêu cầu không hợp lệ";
        };
    }
}
