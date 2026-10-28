package com.lms.dto.quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuizOptionRequest(
        @NotBlank @Size(max = 1000) String content,

        boolean correct) {
}
