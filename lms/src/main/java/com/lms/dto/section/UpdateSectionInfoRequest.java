package com.lms.dto.section;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Phần thông tin giảng viên phụ trách được phép tự chỉnh sửa. */
public record UpdateSectionInfoRequest(
        @NotBlank @Size(max = 255) String name,

        @Size(max = 255) String schedule) {
}
