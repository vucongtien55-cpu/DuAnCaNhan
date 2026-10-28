package com.lms.storage;

import org.springframework.core.io.Resource;

/** Lưu trữ file trên máy chủ. Đường dẫn trả về/nhận vào luôn là đường dẫn tương đối so với thư mục gốc. */
public interface FileStorage {

    /** Lưu file mới; không bao giờ ghi đè file có sẵn. Trả về đường dẫn tương đối để lưu vào database. */
    String store(String subDirectory, String fileName, byte[] content);

    /** @throws com.lms.exception.ResourceNotFoundException nếu file không còn trên máy chủ */
    Resource open(String relativePath);

    /** Xóa file; không báo lỗi nếu file đã không còn. */
    void delete(String relativePath);
}
