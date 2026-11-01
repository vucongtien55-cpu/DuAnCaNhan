package com.lms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mô tả OpenAPI. Chỉ có hiệu lực khi {@code app.docs.enabled=true} (springdoc bị tắt trong cấu hình còn lại).
 * Mọi API mặc định cần JWT Bearer; riêng đăng nhập khai báo {@code @SecurityRequirements} rỗng để Swagger UI
 * không gắn token vào request đó.
 */
@Configuration
public class OpenApiConfig {

    static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI lmsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LMS API")
                        .version("v1")
                        .description("API hệ thống quản lý học tập. Đăng nhập bằng POST /api/auth/login, rồi bấm "
                                + "Authorize và dán accessToken (không gồm chữ 'Bearer'). Quyền thực tế được kiểm tra "
                                + "ở máy chủ theo token, không theo dữ liệu client gửi lên."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
