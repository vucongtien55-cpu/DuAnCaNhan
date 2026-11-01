package com.lms.ratelimit;

import org.springframework.stereotype.Component;

/** Các bucket giới hạn request ghi, dựng từ cấu hình. */
@Component
public class RateLimiters {

    private final boolean enabled;
    private final TokenBucketLimiter write;
    private final TokenBucketLimiter upload;

    public RateLimiters(RateLimitProperties properties) {
        this.enabled = properties.enabled();
        this.write = new TokenBucketLimiter(properties.write().burst(), properties.write().perMinute());
        this.upload = new TokenBucketLimiter(properties.upload().burst(), properties.upload().perMinute());
    }

    public boolean enabled() {
        return enabled;
    }

    public TokenBucketLimiter write() {
        return write;
    }

    public TokenBucketLimiter upload() {
        return upload;
    }
}
