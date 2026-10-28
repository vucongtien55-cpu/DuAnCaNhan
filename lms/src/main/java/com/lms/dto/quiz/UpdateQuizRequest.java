package com.lms.dto.quiz;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Cấu hình bài kiểm tra (không gồm câu hỏi). Đã có lượt làm bài thì không đổi được thời lượng và số lần làm. */
public record UpdateQuizRequest(
        @NotBlank @Size(max = 255) String title,

        String description,

        @NotNull @Positive @Max(600) Integer durationMinutes,

        LocalDateTime startTime,

        LocalDateTime endTime,

        @NotNull @Positive @Max(10) Integer maxAttempts,

        boolean shuffleQuestions) {
}
