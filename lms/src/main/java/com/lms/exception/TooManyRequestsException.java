package com.lms.exception;

/** Vượt giới hạn tần suất. {@code retryAfterSeconds} cho client biết bao lâu nữa nên thử lại. */
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(long retryAfterSeconds) {
        super("Bạn thao tác quá nhanh, vui lòng thử lại sau " + retryAfterSeconds + " giây");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
