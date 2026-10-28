package com.lms.dto.discussion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDiscussionRequest(
        @NotBlank @Size(max = 255) String title,

        @NotBlank @Size(max = 10000) String content) {
}
