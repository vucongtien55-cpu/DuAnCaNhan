package com.novamart.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
//File đăng ký tài khoản
public class RegisterRequest {

    // Email đăng nhập, bắt buộc chuẩn định dạng email
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    // Mật khẩu đăng ký, tối thiểu 6 ký tự
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải từ 6 ký tự trở lên")
    private String password;

    // Họ và tên người dùng
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    // Số điện thoại liên hệ (tùy chọn)
    private String phone;
}