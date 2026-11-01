package com.lms.ratelimit;

import com.lms.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Chặn đoán mật khẩu bằng cách đếm lần đăng nhập <b>thất bại</b> trong một cửa sổ thời gian cố định:
 * <ul>
 *   <li>theo cặp (IP, tên đăng nhập): giới hạn thấp; đăng nhập đúng thì xóa bộ đếm của cặp này;</li>
 *   <li>theo IP: giới hạn cao để người dùng chung IP không bị chặn nhầm.</li>
 * </ul>
 * Đăng nhập thành công không bao giờ bị tính, và không có bộ đếm theo riêng tên đăng nhập nên người khác không thể khóa
 * tài khoản của bạn từ IP khác. Trạng thái nằm trong bộ nhớ của instance (xem {@link TokenBucketLimiter} về nhiều
 * instance). Khóa chỉ nằm trong bộ nhớ, không ghi ra log.
 */
@Component
public class LoginAttemptLimiter {

    private static final int MAX_KEYS = 100_000;
    private static final int MAX_USERNAME_LENGTH = 100;

    private static final class Counter {
        final long windowEndNanos;
        int failures;

        Counter(long windowEndNanos) {
            this.windowEndNanos = windowEndNanos;
        }
    }

    private final RateLimitProperties.Login config;
    private final boolean enabled;
    private final LongSupplier nanoTime;
    private final ConcurrentHashMap<String, Counter> accountFailures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> ipFailures = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptLimiter(RateLimitProperties properties) {
        this(properties, System::nanoTime);
    }

    LoginAttemptLimiter(RateLimitProperties properties, LongSupplier nanoTime) {
        this.config = properties.login();
        this.enabled = properties.enabled();
        this.nanoTime = nanoTime;
    }

    /** Ném {@link TooManyRequestsException} nếu IP hoặc cặp (IP, tên đăng nhập) đã vượt giới hạn thất bại. */
    public void checkAllowed(String ip, String username) {
        if (!enabled) {
            return;
        }
        long now = nanoTime.getAsLong();
        long retry = Math.max(retryAfter(ipFailures.get(ip), config.maxFailuresPerIp(), now),
                retryAfter(accountFailures.get(accountKey(ip, username)), config.maxFailuresPerAccount(), now));
        if (retry > 0) {
            throw new TooManyRequestsException(retry);
        }
    }

    public void recordFailure(String ip, String username) {
        if (!enabled) {
            return;
        }
        long now = nanoTime.getAsLong();
        increment(ipFailures, ip, now);
        increment(accountFailures, accountKey(ip, username), now);
    }

    /** Đăng nhập đúng: xóa bộ đếm của cặp (IP, tên đăng nhập); bộ đếm theo IP vẫn giữ để chống dò nhiều tài khoản. */
    public void recordSuccess(String ip, String username) {
        accountFailures.remove(accountKey(ip, username));
    }

    private long retryAfter(Counter counter, int max, long now) {
        if (counter == null) {
            return 0;
        }
        synchronized (counter) {
            if (now >= counter.windowEndNanos || counter.failures < max) {
                return 0;
            }
            return Math.max(1, (counter.windowEndNanos - now + 999_999_999L) / 1_000_000_000L);
        }
    }

    private void increment(ConcurrentHashMap<String, Counter> map, String key, long now) {
        if (map.size() >= MAX_KEYS && !map.containsKey(key)) {
            map.entrySet().removeIf(e -> now >= e.getValue().windowEndNanos);
            if (map.size() >= MAX_KEYS) {
                return; // đầy: không nhớ thêm khóa mới thay vì làm tràn bộ nhớ
            }
        }
        Counter counter = map.compute(key, (k, existing) ->
                existing == null || now >= existing.windowEndNanos
                        ? new Counter(now + config.window().toNanos()) : existing);
        synchronized (counter) {
            counter.failures++;
        }
    }

    private String accountKey(String ip, String username) {
        String name = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (name.length() > MAX_USERNAME_LENGTH) {
            name = name.substring(0, MAX_USERNAME_LENGTH);
        }
        return ip + '|' + name;
    }
}
