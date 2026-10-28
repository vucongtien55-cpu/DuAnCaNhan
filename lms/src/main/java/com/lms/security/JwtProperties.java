package com.lms.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Cấu hình JWT. {@code secret} lấy từ biến môi trường LMS_JWT_SECRET (tối thiểu 32 byte); để trống thì ứng dụng dùng
 * khóa ngẫu nhiên tạm thời.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("1h") Duration accessTokenTtl,
        @DefaultValue("lms") String issuer) {

    /** Không để lộ khóa ký khi bị in ra log. */
    @Override
    public String toString() {
        return "JwtProperties[secret=***, accessTokenTtl=" + accessTokenTtl + ", issuer=" + issuer + "]";
    }
}
