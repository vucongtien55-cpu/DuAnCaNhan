package com.lms.exception;

/**
 * Lượt làm bài đã hết giờ. Được ném sau khi bài đã được tự động chốt, nên service khai báo
 * {@code noRollbackFor} để việc chốt bài vẫn được lưu.
 */
public class AttemptExpiredException extends BusinessException {

    public AttemptExpiredException(String message) {
        super(message);
    }
}
