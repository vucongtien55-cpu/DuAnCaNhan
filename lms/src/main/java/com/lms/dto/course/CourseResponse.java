package com.lms.dto.course;

import com.lms.entity.Course;
import com.lms.entity.enums.CourseStatus;

import java.time.LocalDateTime;

public record CourseResponse(
        Long id,
        String code,
        String title,
        String description,
        Integer credits,
        String thumbnailUrl,
        CourseStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getCode(),
                course.getTitle(),
                course.getDescription(),
                course.getCredits(),
                course.getThumbnailUrl(),
                course.getStatus(),
                course.getCreatedAt(),
                course.getUpdatedAt());
    }
}
