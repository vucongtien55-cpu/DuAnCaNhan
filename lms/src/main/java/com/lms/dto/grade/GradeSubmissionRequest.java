package com.lms.dto.grade;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** {@code feedback} sinh viên thấy sau khi công bố; {@code internalNote} chỉ giảng viên và Admin thấy. */
public record GradeSubmissionRequest(
        @NotNull @PositiveOrZero Double score,

        @Size(max = 5000) String feedback,

        @Size(max = 5000) String internalNote) {
}
