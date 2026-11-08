package com.novamart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
//File thông tin tài khoản
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    // ID người dùng
    private Long id;

    // Email người dùng
    private String email;

    // Họ và tên
    private String fullName;

    // Số điện thoại
    private String phone;

    // Đường dẫn ảnh đại diện
    private String avatarUrl;

    // Trạng thái tài khoản (ACTIVE, LOCKED...)
    private String status;

    // Danh sách vai trò của người dùng
    private Set<String> roles;
}