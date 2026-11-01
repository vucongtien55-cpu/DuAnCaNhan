package com.lms.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Cấu hình mặc định (tắt tài liệu): OpenAPI và Swagger UI không tồn tại và không truy cập được khi chưa đăng nhập. */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@TestPropertySource(properties = {"app.docs.enabled=false", "app.admin.username=", "app.admin.password="})
class OpenApiDisabledIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("Tắt tài liệu: /v3/api-docs và Swagger UI bị từ chối, không lộ cấu trúc API")
    void docsAreClosedByDefault() throws Exception {
        for (String path : new String[]{"/v3/api-docs", "/v3/api-docs/swagger-config", "/swagger-ui.html",
                "/swagger-ui/index.html"}) {
            var response = mockMvc.perform(get(path)).andReturn().getResponse();
            assertThat(response.getStatus()).as(path).isIn(401, 403, 404);
            assertThat(response.getContentAsString()).as(path).doesNotContain("openapi").doesNotContain("paths");
        }
    }
}
