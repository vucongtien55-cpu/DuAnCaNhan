package com.novamart.service;

import com.novamart.dto.response.UserResponse;

public interface UserService {

    // Lấy thông tin tài khoản cá nhân của người dùng đang đăng nhập
    UserResponse getCurrentUserProfile(String email);
}