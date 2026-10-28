package com.lms.dto.assignment;

import com.lms.entity.enums.GradingPolicy;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * {@code dueDate} null là không có hạn. {@code maxSubmissions} null là không giới hạn số lần nộp.
 * {@code allowLateSubmission} null được hiểu là cho phép nộp muộn; {@code gradingPolicy} null là LATEST.
 */
public record CreateAssignmentRequest(
        @NotNull Long sectionId,

        @NotBlank @Size(max = 255) String title,

        String description,

        LocalDateTime dueDate,

        @NotNull @Positive @DecimalMax("1000") Double maxScore,

        @Positive Integer maxSubmissions,

        Boolean allowLateSubmission,

        GradingPolicy gradingPolicy) {
}
