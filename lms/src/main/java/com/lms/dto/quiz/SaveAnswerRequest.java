package com.lms.dto.quiz;

import jakarta.validation.constraints.NotNull;

/** {@code selectedOptionId} null nghĩa là bỏ chọn (để trống câu này). */
public record SaveAnswerRequest(
        @NotNull Long questionId,

        Long selectedOptionId) {
}
