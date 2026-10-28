package com.lms.dto.submission;

import org.springframework.core.io.Resource;

/** Kết quả tải file bài nộp: nội dung kèm tên và kiểu nội dung do server xác định. */
public record SubmissionFile(
        Resource resource,
        String fileName,
        String contentType,
        long size) {
}
