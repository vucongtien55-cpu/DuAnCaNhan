package com.novamart.controller;

import com.novamart.dto.request.LoginRequest;
import com.novamart.dto.request.RegisterRequest;
import com.novamart.dto.response.ApiResponse;
import com.novamart.dto.response.AuthResponse;
import com.novamart.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // Public API: Đăng ký tài khoản Khách hàng mới (ROLE_CUSTOMER)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        String result = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    // Public API: Đăng nhập lấy JWT Access Token
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", authResponse));
    }
}