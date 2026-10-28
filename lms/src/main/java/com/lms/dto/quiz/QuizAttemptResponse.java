package com.lms.dto.quiz;

import com.lms.entity.enums.AttemptStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Nội dung bài kiểm tra cho sinh viên đang làm bài. KHÔNG có đáp án đúng hay lời giải;
 * thời gian còn lại do server tính, sinh viên không cần (và không được) tự đếm giờ.
 */
public record QuizAttemptResponse(
        Long attemptId,
        Long quizId,
        String quizTitle,
        Integer attemptNumber,
        AttemptStatus status,
        LocalDateTime startedAt,
        LocalDateTime deadlineAt,
        long remainingSeconds,
        List<QuestionView> questions,
        /** Các câu đã lưu: questionId -> optionId đã chọn, để tiếp tục làm bài khi mất kết nối. */
        Map<Long, Long> savedAnswers) {

    public record QuestionView(Long id, String content, double score, List<OptionView> options) {
    }

    public record OptionView(Long id, String content) {
    }
}
