package com.novamart.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
//File đăng nhập
public class LoginRequest {

    // Email đăng nhập
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    // Mật khẩu đăng nhập
    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}