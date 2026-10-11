package org.example.ridehailing.banhangonline.controller;

import jakarta.validation.Valid;
import org.example.ridehailing.banhangonline.dto.ApiRequestDtos;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.service.AuthService;
import org.example.ridehailing.banhangonline.service.PasswordRecoveryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordRecoveryService passwordRecoveryService;

    public AuthController(AuthService authService, PasswordRecoveryService passwordRecoveryService) {
        this.authService = authService;
        this.passwordRecoveryService = passwordRecoveryService;
    }

    // Đăng nhập bằng email/mật khẩu và trả access token JWT.
    @PostMapping("/login")
    public ApiResponseDtos.AuthResponse login(@Valid @RequestBody ApiRequestDtos.LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @PostMapping("/password-reset-requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponseDtos.MessageResponse requestPasswordReset(
            @Valid @RequestBody ApiRequestDtos.PasswordResetRequest request) {
        passwordRecoveryService.requestReset(request.email());
        return new ApiResponseDtos.MessageResponse(
                "Nếu email thuộc tài khoản đang hoạt động, hướng dẫn đặt lại sẽ được gửi khi dịch vụ email khả dụng.");
    }

    @PostMapping("/password-resets")
    public ApiResponseDtos.MessageResponse resetPassword(
            @Valid @RequestBody ApiRequestDtos.PasswordResetCompletionRequest request) {
        passwordRecoveryService.resetPassword(request.token(), request.newPassword());
        return new ApiResponseDtos.MessageResponse("Đã đặt lại mật khẩu. Hãy đăng nhập bằng mật khẩu mới.");
    }
}
