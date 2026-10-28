package com.lms.dto.section;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateSectionRequest(
        @NotBlank @Size(max = 255) String name,

        @Size(max = 255) String semester,

        LocalDate startDate,

        LocalDate endDate,

        @NotNull @Positive @Max(1000) Integer maxStudents,

        @Size(max = 255) String schedule) {
}
