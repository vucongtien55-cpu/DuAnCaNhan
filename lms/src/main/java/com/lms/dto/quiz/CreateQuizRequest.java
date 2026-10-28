package com.lms.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@code startTime}/{@code endTime} null là không giới hạn thời điểm mở/đóng. {@code maxAttempts} null là 1 lần.
 * {@code questions} null hoặc rỗng là chưa có câu hỏi (phải có ít nhất một câu mới công bố được).
 */
public record CreateQuizRequest(
        @NotNull Long sectionId,

        @NotBlank @Size(max = 255) String title,

        String description,

        @NotNull @Positive @Max(600) Integer durationMinutes,

        LocalDateTime startTime,

        LocalDateTime endTime,

        @Positive @Max(10) Integer maxAttempts,

        boolean shuffleQuestions,

        @Valid List<@Valid QuizQuestionRequest> questions) {
}
