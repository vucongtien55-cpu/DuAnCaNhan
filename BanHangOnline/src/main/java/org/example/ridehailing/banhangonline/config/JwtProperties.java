package org.example.ridehailing.banhangonline.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

// Gom các tham số JWT; khóa bí mật chỉ được nạp từ biến môi trường hoặc secret manager.
@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        @NotBlank String secretBase64,
        @NotBlank String issuer,
        @NotNull Duration accessTokenTtl) {
}
