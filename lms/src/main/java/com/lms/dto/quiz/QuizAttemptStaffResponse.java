package com.lms.dto.quiz;

import com.lms.entity.enums.AttemptStatus;

import java.time.LocalDateTime;

/** Lượt làm bài cho Admin và giảng viên phụ trách: có điểm kể cả khi chưa công bố. */
public record QuizAttemptStaffResponse(
        Long attemptId,
        Long studentId,
        String studentName,
        Integer attemptNumber,
        AttemptStatus status,
        LocalDateTime startedAt,
        LocalDateTime submittedAt,
        Double score,
        Double maxScore,
        Long gradeId,
        boolean gradePublished) {
}
