package com.lms.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Khi bật {@code app.docs.enabled}: tài liệu OpenAPI/Swagger UI công khai nhưng không mở thêm quyền nào khác. */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@TestPropertySource(properties = {"app.docs.enabled=true", "app.admin.username=", "app.admin.password="})
class OpenApiEnabledIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JsonMapper jsonMapper;

    private MockHttpServletResponse getDocs() throws Exception {
        return mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse();
    }

    private int status(org.springframework.test.web.servlet.RequestBuilder request)
            throws Exception {
        return mockMvc.perform(request).andReturn().getResponse().getStatus();
    }

    @Test
    @DisplayName("GET /v3/api-docs công khai, có sơ đồ bảo mật JWT Bearer và các API chính")
    void apiDocsAvailable() throws Exception {
        MockHttpServletResponse response = getDocs();
        assertThat(response.getStatus()).isEqualTo(200);
        JsonNode docs = jsonMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));

        JsonNode scheme = docs.path("components").path("securitySchemes").path("bearerAuth");
        assertThat(scheme.path("type").asString()).isEqualTo("http");
        assertThat(scheme.path("scheme").asString()).isEqualTo("bearer");
        assertThat(scheme.path("bearerFormat").asString()).isEqualTo("JWT");
        assertThat(docs.path("security").toString()).contains("bearerAuth");

        JsonNode paths = docs.path("paths");
        assertThat(paths.has("/api/auth/login")).isTrue();
        assertThat(paths.has("/api/users")).isTrue();
        assertThat(paths.has("/api/sections/{sectionId}/student-candidates")).isTrue();
        assertThat(paths.has("/api/courses")).isTrue();
        assertThat(paths.has("/api/quiz-attempts/{attemptId}/submit")).isTrue();
    }

    @Test
    @DisplayName("Đăng nhập không yêu cầu token trong tài liệu; các API khác kế thừa yêu cầu Bearer")
    void loginIsPublicInDocs() throws Exception {
        JsonNode docs = jsonMapper.readTree(getDocs().getContentAsString(StandardCharsets.UTF_8));
        JsonNode login = docs.path("paths").path("/api/auth/login").path("post");
        assertThat(login.path("security").isArray()).isTrue();
        assertThat(login.path("security")).isEmpty();
        assertThat(docs.path("paths").path("/api/users").path("get").has("security")).isFalse();
    }

    @Test
    @DisplayName("Phân trang hiển thị thành page/size/sort (không phải một tham số 'pageable'); bộ lọc tìm người dùng có mặt")
    void paginationAndFiltersDocumented() throws Exception {
        JsonNode docs = jsonMapper.readTree(getDocs().getContentAsString(StandardCharsets.UTF_8));
        java.util.List<String> names = new java.util.ArrayList<>();
        docs.path("paths").path("/api/users").path("get").path("parameters")
                .forEach(p -> names.add(p.path("name").asString()));
        assertThat(names).contains("keyword", "role", "enabled", "page", "size", "sort").doesNotContain("pageable");
    }

    @Test
    @DisplayName("Tài liệu không chứa secret; response người dùng không có trường password")
    void docsLeakNothingSensitive() throws Exception {
        String raw = getDocs().getContentAsString(StandardCharsets.UTF_8);
        assertThat(raw.toLowerCase()).doesNotContain("jwt.secret").doesNotContain("lms_jwt_secret")
                .doesNotContain("lms_db").doesNotContain("eyj");
        JsonNode docs = jsonMapper.readTree(raw);
        JsonNode userProps = docs.path("components").path("schemas").path("UserResponse").path("properties");
        assertThat(userProps.has("id")).isTrue();
        assertThat(userProps.has("password")).isFalse();
        JsonNode summaryProps = docs.path("components").path("schemas").path("UserSummaryResponse").path("properties");
        assertThat(summaryProps.propertyNames()).containsExactlyInAnyOrder("id", "username", "fullName");
    }

    @Test
    @DisplayName("Swagger UI tải được; không mở quyền cho API hay đường dẫn khác")
    void swaggerUiServedWithoutOpeningApi() throws Exception {
        assertThat(status(get("/swagger-ui/index.html"))).isEqualTo(200);
        assertThat(status(get("/swagger-ui.html"))).isIn(200, 302);

        // API vẫn cần đăng nhập, đường dẫn lạ vẫn bị từ chối
        assertThat(status(get("/api/users"))).isEqualTo(401);
        assertThat(status(get("/api/courses"))).isEqualTo(401);
        assertThat(status(get("/actuator/env"))).isIn(401, 403, 404);
        assertThat(status(get("/v3/other"))).isIn(401, 403);

        // Chỉ GET được mở cho đường dẫn tài liệu
        assertThat(status(post("/v3/api-docs"))).isIn(401, 403, 405);
        assertThat(status(delete("/swagger-ui/index.html"))).isIn(401, 403, 405);
    }
}
