package com.lms.dto.assignment;

import org.springframework.core.io.Resource;

/** Kết quả tải file đính kèm đề bài: nội dung kèm tên và kiểu nội dung do server xác định. */
public record AssignmentAttachment(
        Resource resource,
        String fileName,
        String contentType,
        long size) {
}
