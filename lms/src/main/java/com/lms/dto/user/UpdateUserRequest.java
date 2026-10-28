package com.lms.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Email @Size(max = 255) String email,

        @NotBlank @Size(max = 255) String fullName,

        @Size(max = 20) String phone,

        @Size(max = 255) String avatarUrl) {
}
