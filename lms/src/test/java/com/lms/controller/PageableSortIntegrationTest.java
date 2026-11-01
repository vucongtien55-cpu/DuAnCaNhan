package com.lms.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class PageableSortIntegrationTest extends ApiTestSupport {

    private List<String> values(MockHttpServletResponse response, String field) throws Exception {
        assertThat(response.getStatus()).isEqualTo(200);
        List<String> result = new ArrayList<>();
        for (JsonNode item : json(response).path("content")) {
            result.add(item.path(field).asString());
        }
        return result;
    }

    private void assertRejected(String url, Account who) throws Exception {
        MockHttpServletResponse r = call(get(url), who, null);
        assertThat(r.getStatus()).as(url).isEqualTo(400);
        JsonNode body = json(r);
        assertThat(body.path("code").asString()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.path("details").get(0).path("field").asString()).isEqualTo("sort");
        // không phản chiếu nội dung nhạy cảm và không lộ lỗi SQL/JPA
        assertThat(r.getContentAsString()).doesNotContain("PropertyReference").doesNotContain("hibernate");
    }

    @Test
    @DisplayName("Users: sort theo trường công khai hoạt động và thật sự sắp xếp")
    void usersValidSort() throws Exception {
        List<String> asc = values(call(get("/api/users?sort=username,asc&size=50"), admin, null), "username");
        assertThat(asc).isNotEmpty().isSorted();
        List<String> desc = values(call(get("/api/users?sort=username,desc&size=50"), admin, null), "username");
        assertThat(desc).isNotEmpty().isSortedAccordingTo(java.util.Comparator.reverseOrder());
        for (String field : new String[]{"id", "email", "fullName", "enabled", "createdAt"}) {
            assertThat(call(get("/api/users?sort=" + field), admin, null).getStatus()).as(field).isEqualTo(200);
        }
        assertThat(call(get("/api/users"), admin, null).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Users: sort theo password, trường nội bộ hoặc quan hệ lồng -> 400 VALIDATION_ERROR (kể cả khi lẫn trường hợp lệ)")
    void usersInvalidSort() throws Exception {
        assertRejected("/api/users?sort=password", admin);
        assertRejected("/api/users?sort=password,desc", admin);
        assertRejected("/api/users?sort=PASSWORD", admin);
        assertRejected("/api/users?sort=username,asc&sort=password,desc", admin);
        assertRejected("/api/users?sort=roles.name", admin);
        assertRejected("/api/users?sort=roles.id,desc", admin);
        assertRejected("/api/users?sort=nonExistentField", admin);
        assertRejected("/api/users?sort=updatedAt", admin);
    }

    @Test
    @DisplayName("Users: người không phải Admin vẫn bị 403 trước khi xét sort; chưa đăng nhập 401")
    void usersAuthorizationPrecedesSort() throws Exception {
        assertThat(call(get("/api/users?sort=password"), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(get("/api/users?sort=password"), null, null).getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Courses: sort hợp lệ hoạt động; sort theo quan hệ hoặc trường lạ -> 400")
    void courses() throws Exception {
        for (String field : new String[]{"code", "title", "credits", "status", "createdAt", "updatedAt", "id"}) {
            assertThat(call(get("/api/courses?sort=" + field + ",desc"), admin, null).getStatus()).as(field)
                    .isEqualTo(200);
        }
        assertThat(call(get("/api/courses?sort=title&size=50"), studentIn, null).getStatus()).isEqualTo(200);
        assertRejected("/api/courses?sort=sections", studentIn);
        assertRejected("/api/courses?sort=description", admin);
        assertRejected("/api/courses?sort=title,asc&sort=sections.teacher.password", admin);
    }

    @Test
    @DisplayName("Sections: sort hợp lệ hoạt động; sort theo giảng viên/mật khẩu/quan hệ -> 400")
    void sections() throws Exception {
        for (String field : new String[]{"sectionCode", "name", "semester", "startDate", "endDate", "maxStudents",
                "status", "createdAt", "id"}) {
            assertThat(call(get("/api/sections?sort=" + field), admin, null).getStatus()).as(field).isEqualTo(200);
        }
        assertRejected("/api/sections?sort=teacher.password", admin);
        assertRejected("/api/sections?sort=teacher.password", studentIn);
        assertRejected("/api/sections?sort=teacher", lecturerA);
        assertRejected("/api/sections?sort=enrollments.student.password,desc", admin);
        assertRejected("/api/sections?sort=schedule", admin);
    }

    @Test
    @DisplayName("Notifications: tiêu chí sort từ client bị bỏ qua (luôn mới nhất trước), không gây lỗi hay lộ trường nội bộ")
    void notificationsIgnoreClientSort() throws Exception {
        call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/sections/" + sectionId + "/announcements"), lecturerA,
                "{\"title\":\"A\",\"message\":\"m\"}");
        assertThat(call(get("/api/notifications?sort=recipient.password,desc"), studentIn, null).getStatus())
                .isEqualTo(200);
        assertThat(call(get("/api/notifications?sort=eventKey"), studentIn, null).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Phân trang: size quá lớn bị giới hạn tối đa 100; page âm hoặc size 0 không làm hỏng API")
    void pageSizeIsCapped() throws Exception {
        MockHttpServletResponse r = call(get("/api/users?size=100000"), admin, null);
        assertThat(r.getStatus()).isEqualTo(200);
        JsonNode body = json(r);
        assertThat(body.path("content").size()).isLessThanOrEqualTo(100);
        JsonNode page = body.has("page") ? body.path("page") : body;
        assertThat(page.path("size").asInt()).isLessThanOrEqualTo(100);
        assertThat(call(get("/api/users?page=-1&size=0"), admin, null).getStatus()).isEqualTo(200);
    }
}
