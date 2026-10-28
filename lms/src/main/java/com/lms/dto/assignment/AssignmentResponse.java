package com.lms.dto.assignment;

import com.lms.entity.Assignment;
import com.lms.entity.User;
import com.lms.entity.enums.GradingPolicy;

import java.time.LocalDateTime;

public record AssignmentResponse(
        Long id,
        Long sectionId,
        String sectionCode,
        String title,
        String description,
        LocalDateTime dueDate,
        Double maxScore,
        Integer maxSubmissions,
        boolean allowLateSubmission,
        GradingPolicy gradingPolicy,
        String attachmentFileName,
        Long attachmentSize,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt) {

    public static AssignmentResponse from(Assignment assignment) {
        User creator = assignment.getCreatedBy();
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getSection().getId(),
                assignment.getSection().getSectionCode(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueDate(),
                assignment.getMaxScore(),
                assignment.getMaxSubmissions(),
                assignment.isAllowLateSubmission(),
                assignment.getGradingPolicy(),
                assignment.getAttachmentFileName(),
                assignment.getAttachmentSize(),
                creator == null ? null : creator.getId(),
                creator == null ? null : creator.getFullName(),
                assignment.getCreatedAt());
    }
}
