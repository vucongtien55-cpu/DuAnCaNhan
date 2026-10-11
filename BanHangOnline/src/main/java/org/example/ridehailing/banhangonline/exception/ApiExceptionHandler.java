package org.example.ridehailing.banhangonline.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.example.ridehailing.banhangonline.exception.PasswordResetUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // Trả danh sách lỗi validation theo field, không trả stack trace cho client.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Dữ liệu đầu vào không hợp lệ");
        problem.setDetail(exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .reduce((left, right) -> left + "; " + right)
                .orElse("Vui lòng kiểm tra dữ liệu gửi lên."));
        return problem;
    }

    // Xử lý lỗi validation áp dụng trực tiếp trên tham số/path variable.
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Dữ liệu đầu vào không hợp lệ");
        problem.setDetail(exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .distinct()
                .reduce((left, right) -> left + "; " + right)
                .orElse("Vui lòng kiểm tra dữ liệu gửi lên."));
        return problem;
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Vi phạm quy tắc nghiệp vụ", exception.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException exception) {
        return problem(HttpStatus.CONFLICT, "Dữ liệu đã tồn tại", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ", exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu", exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleForbidden(AccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Không đủ quyền truy cập", exception.getMessage());
    }

    @ExceptionHandler(PasswordResetUnavailableException.class)
    public ProblemDetail handlePasswordResetUnavailable(PasswordResetUnavailableException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Khôi phục mật khẩu chưa khả dụng",
                exception.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException exception) {
        if (exception instanceof LockedException) {
            return problem(HttpStatus.LOCKED, "Tài khoản đã bị khóa",
                    "Tài khoản bị khóa sau 5 lần đăng nhập sai. Vui lòng liên hệ quản trị viên.");
        }
        return problem(HttpStatus.UNAUTHORIZED, "Xác thực thất bại",
                "Email hoặc thông tin xác thực không hợp lệ.");
    }

    // Trả 409 khi vi phạm unique/FK giữa lúc kiểm tra và ghi dữ liệu.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataConflict(DataIntegrityViolationException exception) {
        return problem(HttpStatus.CONFLICT, "Dữ liệu bị xung đột",
                "Dữ liệu trùng lặp hoặc đang được tham chiếu bởi dữ liệu khác.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Thiếu tham số",
                "Thiếu tham số bắt buộc: " + exception.getParameterName() + ".");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Sai kiểu dữ liệu",
                "Tham số " + exception.getName() + " không đúng kiểu dữ liệu.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Body không hợp lệ",
                "JSON thiếu, sai định dạng hoặc chứa giá trị không hợp lệ.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type không được hỗ trợ",
                "Hãy gửi dữ liệu JSON với Content-Type: application/json.");
    }

    // Ghi chi tiết lỗi nội bộ vào log; không tiết lộ SQL, token hay stack trace trong phản hồi.
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        LOGGER.error("Lỗi không mong đợi khi xử lý API", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi máy chủ",
                "Đã xảy ra lỗi nội bộ. Vui lòng thử lại sau.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail == null ? title : detail);
        problem.setTitle(title);
        return problem;
    }
}
