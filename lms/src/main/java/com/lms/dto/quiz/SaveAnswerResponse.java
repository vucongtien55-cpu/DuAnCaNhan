package com.lms.dto.quiz;

public record SaveAnswerResponse(
        Long questionId,
        Long selectedOptionId,
        long remainingSeconds) {
}
