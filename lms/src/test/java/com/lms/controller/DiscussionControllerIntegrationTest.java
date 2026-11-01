package com.lms.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class DiscussionControllerIntegrationTest extends ApiTestSupport {

    private long createDiscussion(Account who) throws Exception {
        MockHttpServletResponse r = call(post("/api/discussions"), who,
                "{\"sectionId\":" + sectionId + ",\"title\":\"Hoi bai\",\"content\":\"Noi dung cau hoi\"}");
        assertThat(r.getStatus()).isEqualTo(201);
        return json(r).path("id").asLong();
    }

    private String list() {
        return "/api/sections/" + sectionId + "/discussions";
    }

    @Test
    @DisplayName("Discussion: chưa đăng nhập -> 401 trên mọi endpoint")
    void requireAuthentication() throws Exception {
        assertThat(call(get(list()), null, null).getStatus()).isEqualTo(401);
        assertThat(call(get("/api/discussions/1"), null, null).getStatus()).isEqualTo(401);
        assertThat(call(post("/api/discussions"), null, "{}").getStatus()).isEqualTo(401);
        assertThat(call(put("/api/discussions/1"), null, "{}").getStatus()).isEqualTo(401);
        assertThat(call(delete("/api/discussions/1"), null, null).getStatus()).isEqualTo(401);
        assertThat(call(put("/api/discussions/1/pinned"), null, "{\"pinned\":true}").getStatus()).isEqualTo(401);
        assertThat(call(post("/api/discussions/1/replies"), null, "{}").getStatus()).isEqualTo(401);
        assertThat(call(put("/api/discussion-replies/1"), null, "{}").getStatus()).isEqualTo(401);
        assertThat(call(delete("/api/discussion-replies/1"), null, null).getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Discussion tạo/xem: thành viên lớp (sinh viên ghi danh, giảng viên phụ trách, Admin) tạo và xem được; tác giả lấy từ JWT; người ngoài lớp 403")
    void classMembersOnly() throws Exception {
        MockHttpServletResponse created = call(post("/api/discussions"), studentIn,
                "{\"sectionId\":" + sectionId + ",\"title\":\"Hoi bai\",\"content\":\"Noi dung\","
                        + "\"authorId\":" + lecturerA.id() + "}");
        assertThat(created.getStatus()).isEqualTo(201);
        assertThat(json(created).path("authorId").asLong()).isEqualTo(studentIn.id());
        long id = json(created).path("id").asLong();

        String body = "{\"sectionId\":" + sectionId + ",\"title\":\"X\",\"content\":\"Y\"}";
        assertThat(call(post("/api/discussions"), studentOut, body).getStatus()).isEqualTo(403);
        assertThat(call(post("/api/discussions"), lecturerB, body).getStatus()).isEqualTo(403);
        assertThat(call(post("/api/discussions"), lecturerA, body).getStatus()).isEqualTo(201);
        assertThat(call(post("/api/discussions"), admin, body).getStatus()).isEqualTo(201);
        assertThat(call(post("/api/discussions"), studentIn, "{\"sectionId\":" + sectionId + "}").getStatus())
                .isEqualTo(400);

        assertThat(call(get(list()), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(get(list()), lecturerB, null).getStatus()).isEqualTo(403);
        assertThat(json(call(get(list()), studentIn, null)).size()).isEqualTo(3);
        assertThat(call(get("/api/discussions/" + id), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(get("/api/discussions/" + id), lecturerB, null).getStatus()).isEqualTo(403);
        assertThat(call(get("/api/discussions/" + id), lecturerA, null).getStatus()).isEqualTo(200);
        assertThat(call(get("/api/discussions/999999999"), admin, null).getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("Discussion sửa/xóa: chỉ tác giả hoặc Admin; sinh viên khác và giảng viên không phải tác giả bị 403; chủ đề đã có phản hồi không xóa được")
    void editDeleteOwnership() throws Exception {
        long id = createDiscussion(studentIn);
        String url = "/api/discussions/" + id;
        String update = "{\"title\":\"Sua\",\"content\":\"Noi dung moi\"}";

        assertThat(call(put(url), lecturerA, update).getStatus()).isEqualTo(403);
        assertThat(call(put(url), studentOut, update).getStatus()).isEqualTo(403);
        assertThat(call(put(url), studentIn, "{\"title\":\"\",\"content\":\"\"}").getStatus()).isEqualTo(400);
        assertThat(json(call(put(url), studentIn, update)).path("title").asString()).isEqualTo("Sua");
        assertThat(call(put(url), admin, update).getStatus()).isEqualTo(200);

        assertThat(call(post(url + "/replies"), lecturerA, "{\"content\":\"Tra loi\"}").getStatus())
                .isEqualTo(201);
        assertThat(call(delete(url), studentIn, null).getStatus()).isEqualTo(400);
        assertThat(call(delete(url), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(delete(url), lecturerA, null).getStatus()).isEqualTo(403);
        assertThat(call(delete(url), admin, null).getStatus()).isEqualTo(204);

        long second = createDiscussion(studentIn);
        assertThat(call(delete("/api/discussions/" + second), studentIn, null).getStatus()).isEqualTo(204);
    }

    @Test
    @DisplayName("Discussion ghim/khóa: chỉ giảng viên phụ trách/Admin; sinh viên và giảng viên lớp khác 403; chủ đề khóa thì sinh viên không trả lời được")
    void pinAndLock() throws Exception {
        long id = createDiscussion(studentIn);
        String pin = "/api/discussions/" + id + "/pinned";
        String lock = "/api/discussions/" + id + "/locked";

        assertThat(call(put(pin), studentIn, "{\"pinned\":true}").getStatus()).isEqualTo(403);
        assertThat(call(put(pin), lecturerB, "{\"pinned\":true}").getStatus()).isEqualTo(403);
        assertThat(call(put(pin), lecturerA, "{}").getStatus()).isEqualTo(400);
        assertThat(json(call(put(pin), lecturerA, "{\"pinned\":true}")).path("pinned").asBoolean()).isTrue();
        assertThat(json(call(get(list()), studentIn, null)).get(0).path("pinned").asBoolean()).isTrue();

        assertThat(call(put(lock), studentIn, "{\"locked\":true}").getStatus()).isEqualTo(403);
        assertThat(call(put(lock), lecturerB, "{\"locked\":true}").getStatus()).isEqualTo(403);
        assertThat(json(call(put(lock), lecturerA, "{\"locked\":true}")).path("locked").asBoolean()).isTrue();
        assertThat(call(post("/api/discussions/" + id + "/replies"), studentIn, "{\"content\":\"Con tra loi?\"}")
                .getStatus()).isEqualTo(400);
        assertThat(call(post("/api/discussions/" + id + "/replies"), admin, "{\"content\":\"Admin van duoc\"}")
                .getStatus()).isEqualTo(201);
    }

    @Test
    @DisplayName("Discussion phản hồi: thành viên lớp trả lời (tác giả theo JWT), tối đa hai cấp; chỉ tác giả/Admin sửa-xóa; người ngoài lớp 403")
    void replies() throws Exception {
        long id = createDiscussion(studentIn);
        String replies = "/api/discussions/" + id + "/replies";

        assertThat(call(post(replies), studentOut, "{\"content\":\"Xin chao\"}").getStatus()).isEqualTo(403);
        assertThat(call(post(replies), lecturerB, "{\"content\":\"Xin chao\"}").getStatus()).isEqualTo(403);
        assertThat(call(post(replies), studentIn, "{\"content\":\"\"}").getStatus()).isEqualTo(400);

        MockHttpServletResponse first = call(post(replies), lecturerA,
                "{\"content\":\"Tra loi cua thay\",\"authorId\":" + studentIn.id() + "}");
        assertThat(first.getStatus()).isEqualTo(201);
        assertThat(json(first).path("authorId").asLong()).isEqualTo(lecturerA.id());
        long replyId = json(first).path("id").asLong();

        MockHttpServletResponse nested = call(post(replies), studentIn,
                "{\"content\":\"Cam on thay\",\"parentReplyId\":" + replyId + "}");
        assertThat(nested.getStatus()).isEqualTo(201);
        long nestedId = json(nested).path("id").asLong();
        assertThat(call(post(replies), studentIn,
                "{\"content\":\"Cap ba\",\"parentReplyId\":" + nestedId + "}").getStatus()).isEqualTo(400);

        String edit = "/api/discussion-replies/" + replyId;
        assertThat(call(put(edit), null, "{\"content\":\"Sua\"}").getStatus()).isEqualTo(401);
        assertThat(call(put(edit), studentIn, "{\"content\":\"Sua\"}").getStatus()).isEqualTo(403);
        assertThat(call(put(edit), studentOut, "{\"content\":\"Sua\"}").getStatus()).isEqualTo(403);
        assertThat(call(put(edit), lecturerA, "{\"content\":\"\"}").getStatus()).isEqualTo(400);
        assertThat(json(call(put(edit), lecturerA, "{\"content\":\"Da sua\"}")).path("content").asString())
                .isEqualTo("Da sua");

        assertThat(call(delete(edit), lecturerA, null).getStatus()).isEqualTo(400);
        assertThat(call(delete("/api/discussion-replies/" + nestedId), lecturerA, null).getStatus()).isEqualTo(403);
        assertThat(call(delete("/api/discussion-replies/" + nestedId), studentIn, null).getStatus())
                .isEqualTo(204);
        assertThat(call(delete(edit), lecturerA, null).getStatus()).isEqualTo(204);
        assertThat(call(delete("/api/discussion-replies/999999999"), admin, null).getStatus()).isEqualTo(404);
    }
}
