package com.lms.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Các origin của frontend được phép gọi API (không dùng '*'). Trống nghĩa là chỉ cho cùng origin. */
@ConfigurationProperties(prefix = "app.security.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        if (allowedOrigins.contains("*")) {
            throw new IllegalStateException("app.security.cors.allowed-origins không được dùng '*', "
                    + "hãy liệt kê cụ thể các origin của frontend");
        }
    }
}
