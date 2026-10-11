package org.example.ridehailing.banhangonline.service;

import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;

public interface AuthService {

    // Kiểm tra thông tin đăng nhập và cấp access token JWT ngắn hạn.
    ApiResponseDtos.AuthResponse login(String email, String password);
}
