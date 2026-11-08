package com.novamart.service;

import com.novamart.dto.request.LoginRequest;
import com.novamart.dto.request.RegisterRequest;
import com.novamart.dto.response.AuthResponse;

public interface AuthService {

    // Đăng ký tài khoản khách hàng (ROLE_CUSTOMER)
    String register(RegisterRequest request);

    // Đăng nhập hệ thống và trả về JWT Token
    AuthResponse login(LoginRequest request);
}