package com.lms.dto.user;

import com.lms.entity.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "chỉ gồm chữ, số, '.', '_' và '-'")
        String username,

        @NotBlank @Email @Size(max = 255) String email,

        @NotBlank @Size(min = 8, max = 100) String password,

        @NotBlank @Size(max = 255) String fullName,

        @Size(max = 20) String phone,

        @NotEmpty Set<RoleName> roles) {
}
