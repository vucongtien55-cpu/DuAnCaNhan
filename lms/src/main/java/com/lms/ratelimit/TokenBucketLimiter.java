package com.lms.ratelimit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Token bucket theo khóa, <b>lưu trong bộ nhớ của một instance</b>: chạy nhiều instance thì mỗi instance đếm riêng, giới
 * hạn thực tế là (số instance × giới hạn cấu hình). Muốn giới hạn chính xác toàn cụm cần kho dùng chung (Redis/Bucket4j)
 * hoặc giới hạn ở API gateway/reverse proxy; lớp này cố ý nhỏ gọn để dễ thay thế.
 *
 * <p>Bộ nhớ được chặn trên: bucket đã hồi đầy (không còn trạng thái đáng nhớ) bị dọn định kỳ và khi chạm {@code maxKeys};
 * nếu vẫn đầy thì khóa mới được cho qua (fail-open) thay vì làm tràn bộ nhớ hay chặn nhầm người dùng hợp lệ.
 */
public final class TokenBucketLimiter {

    public record Decision(boolean allowed, long retryAfterSeconds) {
        static final Decision ALLOWED = new Decision(true, 0);
    }

    private static final class Bucket {
        double tokens;
        long lastNanos;

        Bucket(double tokens, long lastNanos) {
            this.tokens = tokens;
            this.lastNanos = lastNanos;
        }
    }

    private static final long NANOS_PER_MINUTE = 60_000_000_000L;
    private static final int EVICT_EVERY_N_CALLS = 1024;

    private final int capacity;
    private final double refillPerNano;
    private final LongSupplier nanoTime;
    private final int maxKeys;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong calls = new AtomicLong();

    public TokenBucketLimiter(int burst, int perMinute) {
        this(burst, perMinute, System::nanoTime, 100_000);
    }

    TokenBucketLimiter(int burst, int perMinute, LongSupplier nanoTime, int maxKeys) {
        if (burst < 1 || perMinute < 1) {
            throw new IllegalArgumentException("burst và perMinute phải dương");
        }
        this.capacity = burst;
        this.refillPerNano = (double) perMinute / NANOS_PER_MINUTE;
        this.nanoTime = nanoTime;
        this.maxKeys = maxKeys;
    }

    public Decision tryAcquire(String key) {
        long now = nanoTime.getAsLong();
        if (calls.incrementAndGet() % EVICT_EVERY_N_CALLS == 0) {
            evictIdle(now);
        }
        if (buckets.size() >= maxKeys && !buckets.containsKey(key)) {
            evictIdle(now);
            if (buckets.size() >= maxKeys) {
                return Decision.ALLOWED;
            }
        }
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, now));
        synchronized (bucket) {
            bucket.tokens = Math.min(capacity, bucket.tokens + (now - bucket.lastNanos) * refillPerNano);
            bucket.lastNanos = now;
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                return Decision.ALLOWED;
            }
            long waitNanos = (long) Math.ceil((1 - bucket.tokens) / refillPerNano);
            return new Decision(false, Math.max(1, (waitNanos + 999_999_999L) / 1_000_000_000L));
        }
    }

    int size() {
        return buckets.size();
    }

    private void evictIdle(long now) {
        buckets.entrySet().removeIf(entry -> {
            Bucket b = entry.getValue();
            synchronized (b) {
                return b.tokens + (now - b.lastNanos) * refillPerNano >= capacity;
            }
        });
    }
}
