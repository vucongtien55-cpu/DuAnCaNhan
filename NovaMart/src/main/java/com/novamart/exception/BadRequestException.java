package com.novamart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ngoại lệ ném ra khi request không hợp lệ hoặc vi phạm quy tắc nghiệp vụ (ví dụ: tồn kho không đủ, trùng email, Shop chưa duyệt...)
 * Trả về HTTP Status 400 BAD REQUEST.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}