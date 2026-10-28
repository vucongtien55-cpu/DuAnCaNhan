package com.lms.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code username} nhận cả tên đăng nhập lẫn email. */
public record LoginRequest(
        @NotBlank @Size(max = 255) String username,

        @NotBlank @Size(max = 100) String password) {

    /** Không để lộ mật khẩu khi đối tượng bị in ra log. */
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
