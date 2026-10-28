package com.lms.dto.course;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
        @NotBlank @Size(max = 30)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "chỉ gồm chữ, số, '.', '_' và '-'")
        String code,

        @NotBlank @Size(max = 255) String title,

        String description,

        @Positive @Max(20) Integer credits,

        @Size(max = 255) String thumbnailUrl) {
}
