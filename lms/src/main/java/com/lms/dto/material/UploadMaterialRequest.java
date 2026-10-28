package com.lms.dto.material;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UploadMaterialRequest(
        @NotBlank @Size(max = 255) String title,

        String description) {
}
