package com.novamart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
//File kết quả đăng nhập
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    // Access Token JWT dùng để xác thực các request sau
    private String accessToken;

    // Loại token, mặc định là Bearer
    @Builder.Default
    private String tokenType = "Bearer";

    // Email tài khoản
    private String email;

    // Họ tên người dùng
    private String fullName;

    // Danh sách các vai trò (ROLE_CUSTOMER, ROLE_SELLER, ROLE_ADMIN)
    private Set<String> roles;
}