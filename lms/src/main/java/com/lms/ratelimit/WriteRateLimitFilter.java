package com.lms.ratelimit;

import com.lms.security.ErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Giới hạn tần suất request ghi (POST/PUT/PATCH/DELETE) theo <b>người dùng đã xác thực</b>; khóa lấy từ
 * SecurityContext (JWT), không từ dữ liệu client. Đặt sau bước xác thực trong chuỗi bảo mật. Request đọc (GET) và
 * request chưa đăng nhập không bị đếm ở đây (request chưa đăng nhập bị chặn bởi phân quyền; đăng nhập có bộ giới hạn
 * riêng {@link LoginAttemptLimiter}).
 *
 * <p>Cố ý <b>không</b> phải Spring bean để Boot không tự đăng ký thêm vào chuỗi servlet; {@code SecurityConfig} tự dựng.
 * Log không chứa token, mật khẩu hay nội dung request.
 */
@Slf4j
public class WriteRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final RateLimiters limiters;
    private final ErrorResponseWriter writer;

    public WriteRateLimitFilter(RateLimiters limiters, ErrorResponseWriter writer) {
        this.limiters = limiters;
        this.writer = writer;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !limiters.enabled() || !WRITE_METHODS.contains(request.getMethod())
                || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            chain.doFilter(request, response);
            return;
        }
        String key = "user:" + authentication.getName();
        TokenBucketLimiter.Decision decision = limiters.write().tryAcquire(key);
        if (decision.allowed() && isMultipart(request)) {
            decision = limiters.upload().tryAcquire(key);
        }
        if (decision.allowed()) {
            chain.doFilter(request, response);
            return;
        }
        log.warn("Vượt giới hạn tần suất ghi: userId={}, {} {}", authentication.getName(), request.getMethod(),
                request.getRequestURI());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
        writer.write(request, response, HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS",
                "Bạn thao tác quá nhanh, vui lòng thử lại sau " + decision.retryAfterSeconds() + " giây");
    }

    private boolean isMultipart(HttpServletRequest request) {
        String type = request.getContentType();
        return type != null && type.regionMatches(true, 0, "multipart/", 0, "multipart/".length());
    }
}
