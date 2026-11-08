package com.novamart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    // Trạng thái thao tác (true: thành công, false: thất bại)
    private boolean success;

    // Thông điệp phản hồi gửi đến Client
    private String message;

    // Dữ liệu trả về (có thể là Object, List, hoặc null nếu chỉ cần thông báo)
    private T data;

    // Thời điểm trả về phản hồi
    private LocalDateTime timestamp;

    // Hàm tiện ích tạo nhanh Response Thành công có kèm dữ liệu
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Hàm tiện ích tạo nhanh Response Thành công chỉ với dữ liệu
    public static <T> ApiResponse<T> success(T data) {
        return success("Thao tác thành công", data);
    }

    // Hàm tiện ích tạo nhanh Response Lỗi với thông điệp lỗi
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }
}