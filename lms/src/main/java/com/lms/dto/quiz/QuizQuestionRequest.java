package com.lms.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Mỗi câu hỏi có từ 2 đến 6 lựa chọn và đúng một đáp án đúng. {@code score} null được hiểu là 1 điểm. */
public record QuizQuestionRequest(
        @NotBlank @Size(max = 5000) String content,

        @Positive @DecimalMax("100") Double score,

        @Size(max = 5000) String explanation,

        @Valid List<@Valid QuizOptionRequest> options) {
}
