package org.example.ridehailing.banhangonline.exception;

// Biểu thị dữ liệu hợp lệ về cấu trúc nhưng vi phạm quy tắc nghiệp vụ.
public class BusinessRuleException extends IllegalArgumentException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
