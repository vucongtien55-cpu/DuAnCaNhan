package com.lms.dto.discussion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplyContentRequest(
        @NotBlank @Size(max = 10000) String content) {
}
