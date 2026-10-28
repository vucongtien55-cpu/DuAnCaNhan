package com.lms.dto.enrollment;

import com.lms.entity.Enrollment;
import com.lms.entity.enums.EnrollmentStatus;

import java.time.LocalDateTime;

public record EnrollmentResponse(
        Long id,
        Long sectionId,
        String sectionCode,
        String sectionName,
        String courseCode,
        String courseTitle,
        Long studentId,
        String studentName,
        EnrollmentStatus status,
        LocalDateTime enrolledAt,
        LocalDateTime droppedAt,
        String dropReason) {

    public static EnrollmentResponse from(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getSection().getId(),
                enrollment.getSection().getSectionCode(),
                enrollment.getSection().getName(),
                enrollment.getSection().getCourse().getCode(),
                enrollment.getSection().getCourse().getTitle(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getFullName(),
                enrollment.getStatus(),
                enrollment.getEnrolledAt(),
                enrollment.getDroppedAt(),
                enrollment.getDropReason());
    }
}
