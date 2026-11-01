package com.lms.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class NotificationControllerIntegrationTest extends ApiTestSupport {

    private static final String ANNOUNCEMENT = "{\"title\":\"Nghi hoc\",\"message\":\"Tuan sau nghi\"}";

    private String announcements() {
        return "/api/sections/" + sectionId + "/announcements";
    }

    private long firstNotificationId(Account who) throws Exception {
        MockHttpServletResponse list = call(get("/api/notifications"), who, null);
        assertThat(list.getStatus()).isEqualTo(200);
        JsonNode content = json(list).path("content");
        assertThat(content.size()).isGreaterThanOrEqualTo(1);
        return content.get(0).path("id").asLong();
    }

    @Test
    @DisplayName("Notification: chưa đăng nhập -> 401 trên mọi endpoint")
    void requireAuthentication() throws Exception {
        assertThat(call(get("/api/notifications"), null, null).getStatus()).isEqualTo(401);
        assertThat(call(get("/api/notifications/unread-count"), null, null).getStatus()).isEqualTo(401);
        assertThat(call(get("/api/notifications/1"), null, null).getStatus()).isEqualTo(401);
        assertThat(call(put("/api/notifications/1/read"), null, null).getStatus()).isEqualTo(401);
        assertThat(call(put("/api/notifications/read-all"), null, null).getStatus()).isEqualTo(401);
        assertThat(call(post(announcements()), null, ANNOUNCEMENT).getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Notification gửi thông báo lớp: giảng viên phụ trách/Admin gửi được; sinh viên và giảng viên lớp khác 403; dữ liệu sai 400; lớp không có 404")
    void announceAccess() throws Exception {
        assertThat(call(post(announcements()), studentIn, ANNOUNCEMENT).getStatus()).isEqualTo(403);
        assertThat(call(post(announcements()), lecturerB, ANNOUNCEMENT).getStatus()).isEqualTo(403);
        assertThat(call(post(announcements()), lecturerA, "{\"title\":\"\",\"message\":\"\"}").getStatus())
                .isEqualTo(400);
        assertThat(call(post("/api/sections/999999999/announcements"), lecturerA, ANNOUNCEMENT).getStatus())
                .isEqualTo(404);

        MockHttpServletResponse ok = call(post(announcements()), lecturerA, ANNOUNCEMENT);
        assertThat(ok.getStatus()).isEqualTo(201);
        assertThat(json(ok).path("count").asLong()).isEqualTo(1);
        assertThat(call(post(announcements()), admin, ANNOUNCEMENT).getStatus()).isEqualTo(201);
    }

    @Test
    @DisplayName("Notification: sinh viên ghi danh nhận thông báo, đếm chưa đọc, đánh dấu đã đọc; sinh viên ngoài lớp không nhận")
    void recipientFlow() throws Exception {
        call(post(announcements()), lecturerA, ANNOUNCEMENT);

        assertThat(json(call(get("/api/notifications/unread-count"), studentIn, null)).path("count").asLong())
                .isEqualTo(1);
        assertThat(json(call(get("/api/notifications/unread-count"), studentOut, null)).path("count").asLong())
                .isEqualTo(0);
        assertThat(json(call(get("/api/notifications"), studentOut, null)).path("content").size()).isEqualTo(0);

        long id = firstNotificationId(studentIn);
        MockHttpServletResponse one = call(get("/api/notifications/" + id), studentIn, null);
        assertThat(one.getStatus()).isEqualTo(200);
        assertThat(json(one).path("title").asString()).isEqualTo("Nghi hoc");
        assertThat(json(one).path("read").asBoolean()).isFalse();
        assertThat(json(one).path("sectionId").asLong()).isEqualTo(sectionId);

        MockHttpServletResponse read = call(put("/api/notifications/" + id + "/read"), studentIn, null);
        assertThat(read.getStatus()).isEqualTo(200);
        assertThat(json(read).path("read").asBoolean()).isTrue();
        assertThat(json(call(get("/api/notifications/unread-count"), studentIn, null)).path("count").asLong())
                .isEqualTo(0);
        assertThat(json(call(get("/api/notifications?unreadOnly=true"), studentIn, null)).path("content").size())
                .isEqualTo(0);
    }

    @Test
    @DisplayName("Notification: thông báo của người khác không xem/đánh dấu được (403); không tồn tại 404; read-all chỉ ảnh hưởng thông báo của mình")
    void ownershipIsEnforced() throws Exception {
        call(post(announcements()), lecturerA, ANNOUNCEMENT);
        long id = firstNotificationId(studentIn);

        assertThat(call(get("/api/notifications/" + id), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(get("/api/notifications/" + id), admin, null).getStatus()).isEqualTo(403);
        assertThat(call(put("/api/notifications/" + id + "/read"), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(get("/api/notifications/999999999"), studentIn, null).getStatus()).isEqualTo(404);

        MockHttpServletResponse none = call(put("/api/notifications/read-all"), studentOut, null);
        assertThat(json(none).path("count").asLong()).isEqualTo(0);
        assertThat(json(call(get("/api/notifications/unread-count"), studentIn, null)).path("count").asLong())
                .isEqualTo(1);

        MockHttpServletResponse all = call(put("/api/notifications/read-all"), studentIn, null);
        assertThat(json(all).path("count").asLong()).isEqualTo(1);
    }
}
