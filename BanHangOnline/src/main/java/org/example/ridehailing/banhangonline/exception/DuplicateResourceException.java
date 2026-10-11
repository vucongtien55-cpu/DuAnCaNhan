package org.example.ridehailing.banhangonline.exception;

// Biểu thị tài nguyên bị trùng theo một định danh duy nhất như email hoặc slug.
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
