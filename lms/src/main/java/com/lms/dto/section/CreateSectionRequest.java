package com.lms.dto.section;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** {@code sectionCode} để trống (null) thì hệ thống tự sinh mã; {@code teacherId} có thể gán sau. */
public record CreateSectionRequest(
        @NotNull Long courseId,

        @Size(max = 40)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "chỉ gồm chữ, số, '.', '_' và '-'")
        String sectionCode,

        @NotBlank @Size(max = 255) String name,

        @Size(max = 255) String semester,

        LocalDate startDate,

        LocalDate endDate,

        @NotNull @Positive @Max(1000) Integer maxStudents,

        @Size(max = 255) String schedule,

        Long teacherId) {
}
