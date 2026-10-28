package com.lms.dto.material;

import org.springframework.core.io.Resource;

/** Kết quả tải xuống: nội dung file kèm tên và kiểu nội dung do server xác định. */
public record MaterialFile(
        Resource resource,
        String fileName,
        String contentType,
        long size) {
}
