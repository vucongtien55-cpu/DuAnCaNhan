package com.lms.dto.quiz;

import com.lms.entity.Quiz;

import java.time.LocalDateTime;

/** Thông tin tóm tắt bài kiểm tra, an toàn cho mọi vai trò (không có câu hỏi hay đáp án). */
public record QuizSummaryResponse(
        Long id,
        Long sectionId,
        String title,
        String description,
        Integer durationMinutes,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer maxAttempts,
        boolean published,
        double maxScore,
        int questionCount) {

    public static QuizSummaryResponse from(Quiz quiz) {
        return new QuizSummaryResponse(
                quiz.getId(),
                quiz.getSection().getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getDurationMinutes(),
                quiz.getStartTime(),
                quiz.getEndTime(),
                quiz.getMaxAttempts(),
                quiz.isPublished(),
                QuizResponse.maxScoreOf(quiz),
                quiz.getQuestions().size());
    }
}
