package com.lms.dto.grade;

import com.lms.entity.Assignment;
import com.lms.entity.Grade;

import java.time.LocalDateTime;

/**
 * Điểm chính thức của một bài tập, dành cho sinh viên. Khi chưa công bố chỉ có {@code published = false},
 * không có điểm, nhận xét hay thông tin người chấm; không bao giờ có nhận xét nội bộ.
 */
public record StudentGradeResponse(
        Long assignmentId,
        String assignmentTitle,
        boolean published,
        Double score,
        Double maxScore,
        String feedback,
        Integer attemptNumber,
        boolean late,
        LocalDateTime publishedAt) {

    public static StudentGradeResponse notPublished(Assignment assignment) {
        return new StudentGradeResponse(assignment.getId(), assignment.getTitle(), false,
                null, null, null, null, false, null);
    }

    public static StudentGradeResponse of(Assignment assignment, Grade publishedGrade) {
        return new StudentGradeResponse(
                assignment.getId(),
                assignment.getTitle(),
                true,
                publishedGrade.getScore(),
                publishedGrade.getMaxScore(),
                publishedGrade.getFeedback(),
                publishedGrade.getSubmission().getAttemptNumber(),
                publishedGrade.getSubmission().isLate(),
                publishedGrade.getPublishedAt());
    }
}
