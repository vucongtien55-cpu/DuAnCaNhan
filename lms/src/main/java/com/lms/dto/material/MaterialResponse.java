package com.lms.dto.material;

import com.lms.entity.Material;
import com.lms.entity.User;
import com.lms.entity.enums.MaterialType;

import java.time.LocalDateTime;

/** Không chứa đường dẫn file trên máy chủ. */
public record MaterialResponse(
        Long id,
        Long sectionId,
        String title,
        String description,
        MaterialType type,
        String originalFileName,
        String contentType,
        Long fileSize,
        Long uploadedById,
        String uploadedByName,
        LocalDateTime createdAt) {

    public static MaterialResponse from(Material material) {
        User uploader = material.getUploadedBy();
        return new MaterialResponse(
                material.getId(),
                material.getSection().getId(),
                material.getTitle(),
                material.getDescription(),
                material.getType(),
                material.getOriginalFileName(),
                material.getContentType(),
                material.getFileSize(),
                uploader == null ? null : uploader.getId(),
                uploader == null ? null : uploader.getFullName(),
                material.getCreatedAt());
    }
}
