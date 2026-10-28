package com.lms.dto.submission;

import com.lms.entity.Submission;
import com.lms.entity.enums.SubmissionStatus;

import java.time.LocalDateTime;

/** Không chứa đường dẫn file trên máy chủ. */
public record SubmissionResponse(
        Long id,
        Long assignmentId,
        String assignmentTitle,
        Long studentId,
        String studentName,
        Integer attemptNumber,
        String content,
        String originalFileName,
        String contentType,
        Long fileSize,
        LocalDateTime submittedAt,
        boolean late,
        boolean selectedForGrading,
        SubmissionStatus status) {

    public static SubmissionResponse from(Submission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getAssignment().getTitle(),
                submission.getStudent().getId(),
                submission.getStudent().getFullName(),
                submission.getAttemptNumber(),
                submission.getContent(),
                submission.getOriginalFileName(),
                submission.getContentType(),
                submission.getFileSize(),
                submission.getSubmittedAt(),
                submission.isLate(),
                submission.isSelectedForGrading(),
                submission.getStatus());
    }
}
