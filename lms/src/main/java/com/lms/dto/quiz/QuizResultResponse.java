package com.lms.dto.quiz;

import com.lms.entity.enums.AttemptStatus;

import java.time.LocalDateTime;

/**
 * Kết quả một lượt làm bài cho sinh viên. {@code score}/{@code maxScore} chỉ có giá trị khi điểm đã được công bố;
 * không bao giờ chứa đáp án đúng hay lời giải.
 */
public record QuizResultResponse(
        Long attemptId,
        Long quizId,
        String quizTitle,
        Integer attemptNumber,
        AttemptStatus status,
        LocalDateTime startedAt,
        LocalDateTime submittedAt,
        boolean gradePublished,
        Double score,
        Double maxScore) {
}
