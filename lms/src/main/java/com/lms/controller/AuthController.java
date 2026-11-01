package com.lms.controller;

import com.lms.dto.auth.LoginRequest;
import com.lms.dto.auth.LoginResponse;
import com.lms.dto.user.UserResponse;
import com.lms.ratelimit.LoginAttemptLimiter;
import com.lms.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginAttemptLimiter loginAttemptLimiter;

    /** Công khai. Sai tên đăng nhập/mật khẩu: 401; tài khoản bị khóa (sau khi mật khẩu đúng): 403. */
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        // Chỉ dùng địa chỉ kết nối; khi chạy sau reverse proxy, bật server.forward-headers-strategy để Tomcat tự
        // chuẩn hóa remoteAddr từ proxy tin cậy (không đọc X-Forwarded-For thủ công vì client giả mạo được).
        String ip = http.getRemoteAddr();
        loginAttemptLimiter.checkAllowed(ip, request.username());
        LoginResponse response;
        try {
            response = authService.login(request);
        } catch (BadCredentialsException e) {
            loginAttemptLimiter.recordFailure(ip, request.username());
            throw e;
        }
        loginAttemptLimiter.recordSuccess(ip, request.username());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    /** Thông tin người dùng đang đăng nhập (không có mật khẩu). */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.currentUser(Long.valueOf(jwt.getSubject()));
    }

    /**
     * Thu hồi ngay access token đang dùng. Token của các thiết bị/phiên khác của cùng người dùng không bị ảnh hưởng và
     * vẫn dùng được đến khi hết hạn; frontend cũng phải xóa token ở phía mình.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        authService.logout(jwt.getId(), jwt.getExpiresAt());
        return ResponseEntity.noContent().build();
    }
}
