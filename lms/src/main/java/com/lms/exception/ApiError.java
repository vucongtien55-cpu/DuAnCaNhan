package com.lms.exception;

import java.time.Instant;
import java.util.List;

/**
 * Định dạng lỗi thống nhất của mọi API.
 *
 * @param code    mã lỗi ổn định cho frontend xử lý (NOT_FOUND, FORBIDDEN, VALIDATION_ERROR, ...)
 * @param message thông điệp an toàn để hiển thị; không chứa chi tiết nội bộ
 * @param details lỗi theo từng trường khi dữ liệu đầu vào sai; rỗng nếu không có
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldIssue> details) {

    public record FieldIssue(String field, String message) {
    }

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path, List.of());
    }

    public static ApiError of(int status, String code, String message, String path, List<FieldIssue> details) {
        return new ApiError(Instant.now(), status, code, message, path, details);
    }
}
