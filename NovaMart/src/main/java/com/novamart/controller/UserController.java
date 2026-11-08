package com.novamart.controller;

import com.novamart.dto.response.ApiResponse;
import com.novamart.dto.response.UserResponse;
import com.novamart.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * User API: Lấy thông tin hồ sơ tài khoản của người dùng đang đăng nhập
     * Thông tin Email được trích xuất trực tiếp từ JWT Access Token trong Header.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile(Authentication authentication) {
        UserResponse user = userService.getCurrentUserProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tài khoản thành công", user));
    }
}