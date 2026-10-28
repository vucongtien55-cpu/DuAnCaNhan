package com.lms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Thông tin Admin đầu tiên, đọc từ biến môi trường LMS_ADMIN_USERNAME, LMS_ADMIN_PASSWORD (bắt buộc cả hai) và
 * tùy chọn LMS_ADMIN_EMAIL, LMS_ADMIN_FULL_NAME. Không có giá trị mặc định nào cho tên đăng nhập hay mật khẩu.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminSeedProperties(
        String username,
        String password,
        String email,
        @DefaultValue("Quản trị viên") String fullName) {

    public boolean hasUsername() {
        return username != null && !username.isBlank();
    }

    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }

    /** Không để lộ mật khẩu khi đối tượng bị in ra log. */
    @Override
    public String toString() {
        return "AdminSeedProperties[username=" + username + ", password=***, email=" + email
                + ", fullName=" + fullName + "]";
    }
}
