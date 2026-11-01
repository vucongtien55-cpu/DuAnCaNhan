package com.lms.ratelimit;

import com.lms.exception.TooManyRequestsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterUnitTest {

    private static final long SECOND = 1_000_000_000L;

    private final AtomicLong clock = new AtomicLong(1_000 * SECOND);

    // ---------- TokenBucketLimiter ----------

    @Test
    @DisplayName("Token bucket: cho phép đúng burst rồi từ chối kèm thời gian chờ")
    void bucketDeniesAfterBurst() {
        TokenBucketLimiter limiter = new TokenBucketLimiter(3, 60, clock::get, 100);
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("u1").allowed()).isTrue();
        }
        TokenBucketLimiter.Decision denied = limiter.tryAcquire("u1");
        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterSeconds()).isEqualTo(1); // 60/phút = 1 token/giây
    }

    @Test
    @DisplayName("Token bucket: hồi phục theo thời gian, không vượt quá burst")
    void bucketRefills() {
        TokenBucketLimiter limiter = new TokenBucketLimiter(2, 60, clock::get, 100);
        limiter.tryAcquire("u1");
        limiter.tryAcquire("u1");
        assertThat(limiter.tryAcquire("u1").allowed()).isFalse();

        clock.addAndGet(SECOND);
        assertThat(limiter.tryAcquire("u1").allowed()).isTrue();
        assertThat(limiter.tryAcquire("u1").allowed()).isFalse();

        clock.addAndGet(3_600 * SECOND); // nghỉ rất lâu chỉ hồi tối đa burst
        assertThat(limiter.tryAcquire("u1").allowed()).isTrue();
        assertThat(limiter.tryAcquire("u1").allowed()).isTrue();
        assertThat(limiter.tryAcquire("u1").allowed()).isFalse();
    }

    @Test
    @DisplayName("Token bucket: mỗi khóa độc lập, người dùng này bị chặn không ảnh hưởng người khác")
    void bucketsAreIndependent() {
        TokenBucketLimiter limiter = new TokenBucketLimiter(1, 1, clock::get, 100);
        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        assertThat(limiter.tryAcquire("a").allowed()).isFalse();
        assertThat(limiter.tryAcquire("b").allowed()).isTrue();
    }

    @Test
    @DisplayName("Token bucket: thời gian chờ phản ánh tốc độ hồi phục chậm")
    void retryAfterReflectsRefillRate() {
        TokenBucketLimiter limiter = new TokenBucketLimiter(1, 6, clock::get, 100); // 1 token / 10 giây
        limiter.tryAcquire("a");
        assertThat(limiter.tryAcquire("a").retryAfterSeconds()).isEqualTo(10);
        clock.addAndGet(4 * SECOND);
        assertThat(limiter.tryAcquire("a").retryAfterSeconds()).isEqualTo(6);
    }

    @Test
    @DisplayName("Token bucket: bộ nhớ bị chặn trên, dọn bucket đã hồi đầy, đầy thật thì fail-open thay vì tràn")
    void bucketMemoryIsBounded() {
        TokenBucketLimiter limiter = new TokenBucketLimiter(2, 60, clock::get, 3);
        limiter.tryAcquire("a");
        limiter.tryAcquire("b");
        limiter.tryAcquire("c");
        assertThat(limiter.size()).isEqualTo(3);
        // khóa thứ 4 khi chưa có bucket nào hồi đầy: không lưu thêm, không chặn nhầm
        assertThat(limiter.tryAcquire("d").allowed()).isTrue();
        assertThat(limiter.size()).isEqualTo(3);

        clock.addAndGet(10 * SECOND); // tất cả đã hồi đầy -> được dọn khi cần chỗ
        assertThat(limiter.tryAcquire("e").allowed()).isTrue();
        assertThat(limiter.size()).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Token bucket: cấu hình không hợp lệ bị từ chối")
    void bucketRejectsInvalidConfig() {
        assertThatThrownBy(() -> new TokenBucketLimiter(0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TokenBucketLimiter(10, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- LoginAttemptLimiter ----------

    private LoginAttemptLimiter loginLimiter(boolean enabled, int perAccount, int perIp) {
        RateLimitProperties props = new RateLimitProperties(enabled, new RateLimitProperties.Bucket(60, 120),
                new RateLimitProperties.Upload(10, 20),
                new RateLimitProperties.Login(perAccount, perIp, Duration.ofMinutes(15)));
        return new LoginAttemptLimiter(props, clock::get);
    }

    @Test
    @DisplayName("Login: sau N lần sai liên tiếp thì chặn cặp (IP, tên đăng nhập) kèm Retry-After; tên đăng nhập không phân biệt hoa thường")
    void loginBlockedAfterFailures() {
        LoginAttemptLimiter limiter = loginLimiter(true, 3, 100);
        for (int i = 0; i < 3; i++) {
            assertThatCode(() -> limiter.checkAllowed("1.1.1.1", "SinhVien")).doesNotThrowAnyException();
            limiter.recordFailure("1.1.1.1", i % 2 == 0 ? "SinhVien" : "sinhvien ");
        }
        assertThatThrownBy(() -> limiter.checkAllowed("1.1.1.1", "sinhvien"))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        e -> assertThat(e.getRetryAfterSeconds()).isBetween(1L, 900L));
    }

    @Test
    @DisplayName("Login: không chặn nhầm — tài khoản khác cùng IP và cùng tài khoản từ IP khác vẫn đăng nhập được")
    void loginDoesNotBlockInnocentUsers() {
        LoginAttemptLimiter limiter = loginLimiter(true, 3, 100);
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("1.1.1.1", "nan_nhan");
        }
        assertThatThrownBy(() -> limiter.checkAllowed("1.1.1.1", "nan_nhan")).isInstanceOf(TooManyRequestsException.class);
        assertThatCode(() -> limiter.checkAllowed("1.1.1.1", "nguoi_khac")).doesNotThrowAnyException();
        assertThatCode(() -> limiter.checkAllowed("2.2.2.2", "nan_nhan")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Login: đăng nhập đúng xóa bộ đếm thất bại của cặp, không bị tích lũy dần")
    void loginSuccessResetsCounter() {
        LoginAttemptLimiter limiter = loginLimiter(true, 3, 100);
        for (int round = 0; round < 5; round++) {
            limiter.recordFailure("1.1.1.1", "sv");
            limiter.recordFailure("1.1.1.1", "sv");
            assertThatCode(() -> limiter.checkAllowed("1.1.1.1", "sv")).doesNotThrowAnyException();
            limiter.recordSuccess("1.1.1.1", "sv");
        }
    }

    @Test
    @DisplayName("Login: hết cửa sổ thời gian thì được thử lại")
    void loginWindowExpires() {
        LoginAttemptLimiter limiter = loginLimiter(true, 2, 100);
        limiter.recordFailure("1.1.1.1", "sv");
        limiter.recordFailure("1.1.1.1", "sv");
        assertThatThrownBy(() -> limiter.checkAllowed("1.1.1.1", "sv")).isInstanceOf(TooManyRequestsException.class);

        clock.addAndGet(16 * 60 * SECOND);
        assertThatCode(() -> limiter.checkAllowed("1.1.1.1", "sv")).doesNotThrowAnyException();
        limiter.recordFailure("1.1.1.1", "sv"); // cửa sổ mới bắt đầu lại từ 1
        assertThatCode(() -> limiter.checkAllowed("1.1.1.1", "sv")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Login: giới hạn theo IP chặn việc dò nhiều tài khoản từ một IP (kể cả khi từng tài khoản chưa chạm ngưỡng)")
    void loginIpLimit() {
        LoginAttemptLimiter limiter = loginLimiter(true, 5, 6);
        for (int i = 0; i < 6; i++) {
            limiter.recordFailure("9.9.9.9", "user" + i);
        }
        assertThatThrownBy(() -> limiter.checkAllowed("9.9.9.9", "mot_nguoi_moi"))
                .isInstanceOf(TooManyRequestsException.class);
        assertThatCode(() -> limiter.checkAllowed("8.8.8.8", "mot_nguoi_moi")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Login: khi tắt cấu hình thì không bao giờ chặn")
    void loginDisabled() {
        LoginAttemptLimiter limiter = loginLimiter(false, 1, 1);
        for (int i = 0; i < 10; i++) {
            limiter.recordFailure("1.1.1.1", "sv");
        }
        assertThatCode(() -> limiter.checkAllowed("1.1.1.1", "sv")).doesNotThrowAnyException();
    }
}
