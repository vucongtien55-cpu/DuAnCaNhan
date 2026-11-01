package com.lms.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Giới hạn tần suất (lưu trong bộ nhớ của từng instance, xem {@link TokenBucketLimiter}).
 *
 * @param write  mọi request ghi (POST/PUT/PATCH/DELETE) của một người dùng đã đăng nhập
 * @param upload bổ sung cho request multipart (upload file), tính thêm vào {@code write}
 * @param login  đăng nhập sai liên tiếp
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue Bucket write,
        @DefaultValue Upload upload,
        @DefaultValue Login login) {

    /** {@code burst}: số request tối đa dồn trong một lúc; {@code perMinute}: tốc độ hồi phục bền vững. */
    public record Bucket(@DefaultValue("60") int burst, @DefaultValue("120") int perMinute) {
    }

    public record Upload(@DefaultValue("10") int burst, @DefaultValue("20") int perMinute) {
    }

    /**
     * Chỉ đếm lần đăng nhập thất bại. {@code maxFailuresPerAccount} tính riêng cho từng cặp (IP, tên đăng nhập) nên người
     * khác không thể khóa tài khoản của bạn từ IP của họ; {@code maxFailuresPerIp} cao hơn để nhiều người dùng chung IP
     * (mạng trường học, NAT) không bị chặn nhầm.
     */
    public record Login(@DefaultValue("5") int maxFailuresPerAccount,
                        @DefaultValue("100") int maxFailuresPerIp,
                        @DefaultValue("15m") Duration window) {
    }
}
