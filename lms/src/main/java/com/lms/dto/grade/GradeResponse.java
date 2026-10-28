package com.lms.dto.grade;

import com.lms.entity.Grade;
import com.lms.entity.Submission;
import com.lms.entity.User;
import com.lms.entity.enums.GradeType;

import java.time.LocalDateTime;

/** Dữ liệu điểm đầy đủ cho Admin và giảng viên phụ trách (có nhận xét nội bộ). Không dùng cho sinh viên. */
public record GradeResponse(
        Long id,
        GradeType type,
        Long enrollmentId,
        Long studentId,
        String studentName,
        Long submissionId,
        Long assignmentId,
        Integer attemptNumber,
        boolean late,
        Double score,
        Double maxScore,
        String feedback,
        String internalNote,
        boolean published,
        LocalDateTime publishedAt,
        Long gradedById,
        String gradedByName,
        LocalDateTime gradedAt) {

    public static GradeResponse from(Grade grade) {
        Submission submission = grade.getSubmission();
        User student = grade.getEnrollment().getStudent();
        User grader = grade.getGradedBy();
        return new GradeResponse(
                grade.getId(),
                grade.getType(),
                grade.getEnrollment().getId(),
                student.getId(),
                student.getFullName(),
                submission == null ? null : submission.getId(),
                submission == null ? null : submission.getAssignment().getId(),
                submission == null ? null : submission.getAttemptNumber(),
                submission != null && submission.isLate(),
                grade.getScore(),
                grade.getMaxScore(),
                grade.getFeedback(),
                grade.getInternalNote(),
                grade.isPublished(),
                grade.getPublishedAt(),
                grader == null ? null : grader.getId(),
                grader == null ? null : grader.getFullName(),
                grade.getGradedAt());
    }
}
