package org.example.ridehailing.banhangonline.exception;

import jakarta.persistence.EntityNotFoundException;

// Biểu thị tài nguyên nghiệp vụ không tồn tại; tương thích với các service đang dùng JPA exception.
public class ResourceNotFoundException extends EntityNotFoundException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
