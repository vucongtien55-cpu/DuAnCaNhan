package com.lms.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnouncementRequest(
        @NotBlank @Size(max = 255) String title,

        @NotBlank @Size(max = 2000) String message) {
}
