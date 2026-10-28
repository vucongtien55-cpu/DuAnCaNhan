package com.lms.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String oldPassword,

        @NotBlank @Size(min = 8, max = 100) String newPassword) {
}
