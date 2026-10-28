package com.lms.dto.discussion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDiscussionRequest(
        @NotNull Long sectionId,

        @NotBlank @Size(max = 255) String title,

        @NotBlank @Size(max = 10000) String content) {
}
