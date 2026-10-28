package com.lms.dto.assignment;

import com.lms.entity.enums.GradingPolicy;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateAssignmentRequest(
        @NotBlank @Size(max = 255) String title,

        String description,

        LocalDateTime dueDate,

        @NotNull @Positive @DecimalMax("1000") Double maxScore,

        @Positive Integer maxSubmissions,

        @NotNull Boolean allowLateSubmission,

        @NotNull GradingPolicy gradingPolicy) {
}
