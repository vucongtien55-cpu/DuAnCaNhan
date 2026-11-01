package com.lms.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class GradeControllerIntegrationTest extends ApiTestSupport {

    private long assignmentId;
    private long submissionId;

    private void prepareSubmission() throws Exception {
        assignmentId = createAssignment("");
        MockHttpServletResponse r = upload("/api/assignments/" + assignmentId + "/submissions", false, studentIn,
                "bai.pdf", PDF);
        assertThat(r.getStatus()).isEqualTo(201);
        submissionId = json(r).path("id").asLong();
    }

    private String gradeUrl() {
        return "/api/submissions/" + submissionId + "/grade";
    }

    private static final String GRADE = "{\"score\":8.5,\"feedback\":\"Tot\",\"internalNote\":\"ghi chu noi bo\"}";

    @Test
    @DisplayName("Grade chấm điểm: giảng viên phụ trách/Admin chấm thành công; chưa đăng nhập 401; sinh viên và giảng viên lớp khác 403")
    void gradingAccess() throws Exception {
        prepareSubmission();
        assertThat(call(put(gradeUrl()), null, GRADE).getStatus()).isEqualTo(401);
        assertThat(call(put(gradeUrl()), studentIn, GRADE).getStatus()).isEqualTo(403);
        assertThat(call(put(gradeUrl()), studentOut, GRADE).getStatus()).isEqualTo(403);
        assertThat(call(put(gradeUrl()), lecturerB, GRADE).getStatus()).isEqualTo(403);

        MockHttpServletResponse ok = call(put(gradeUrl()), lecturerA, GRADE);
        assertThat(ok.getStatus()).isEqualTo(200);
        assertThat(json(ok).path("score").asDouble()).isEqualTo(8.5);
        assertThat(json(ok).path("published").asBoolean()).isFalse();
        assertThat(json(ok).path("gradedById").asLong()).isEqualTo(lecturerA.id());
        assertThat(call(put(gradeUrl()), admin, "{\"score\":9}").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Grade: gradedById/published/studentId do client gửi lên bị bỏ qua; điểm âm/vượt tối đa/thiếu -> 400; bài nộp không có -> 404")
    void gradingValidation() throws Exception {
        prepareSubmission();
        MockHttpServletResponse r = call(put(gradeUrl()), lecturerA,
                "{\"score\":7,\"gradedById\":" + studentIn.id() + ",\"published\":true,\"studentId\":"
                        + studentOut.id() + "}");
        assertThat(r.getStatus()).isEqualTo(200);
        assertThat(json(r).path("gradedById").asLong()).isEqualTo(lecturerA.id());
        assertThat(json(r).path("published").asBoolean()).isFalse();
        assertThat(json(r).path("studentId").asLong()).isEqualTo(studentIn.id());

        assertThat(call(put(gradeUrl()), lecturerA, "{\"score\":-1}").getStatus()).isEqualTo(400);
        assertThat(call(put(gradeUrl()), lecturerA, "{\"score\":10.5}").getStatus()).isEqualTo(400);
        assertThat(call(put(gradeUrl()), lecturerA, "{}").getStatus()).isEqualTo(400);
        assertThat(call(put("/api/submissions/999999999/grade"), lecturerA, GRADE).getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("Grade: sinh viên không thấy điểm trước khi công bố; sau công bố thấy điểm + nhận xét nhưng không có ghi chú nội bộ")
    void studentSeesOnlyPublishedGrade() throws Exception {
        prepareSubmission();
        call(put(gradeUrl()), lecturerA, GRADE);
        String myGrade = "/api/assignments/" + assignmentId + "/my-grade";

        assertThat(call(get(myGrade), null, null).getStatus()).isEqualTo(401);
        MockHttpServletResponse before = call(get(myGrade), studentIn, null);
        assertThat(before.getStatus()).isEqualTo(200);
        assertThat(json(before).path("published").asBoolean()).isFalse();
        assertThat(json(before).path("score").isNull()).isTrue();

        assertThat(call(post("/api/assignments/" + assignmentId + "/grades/publish"), studentIn, null).getStatus())
                .isEqualTo(403);
        assertThat(call(post("/api/assignments/" + assignmentId + "/grades/publish"), lecturerB, null).getStatus())
                .isEqualTo(403);
        MockHttpServletResponse published = call(post("/api/assignments/" + assignmentId + "/grades/publish"),
                lecturerA, null);
        assertThat(published.getStatus()).isEqualTo(200);
        assertThat(json(published).path("count").asInt()).isEqualTo(1);

        MockHttpServletResponse after = call(get(myGrade), studentIn, null);
        JsonNode body = json(after);
        assertThat(body.path("published").asBoolean()).isTrue();
        assertThat(body.path("score").asDouble()).isEqualTo(8.5);
        assertThat(body.path("feedback").asString()).isEqualTo("Tot");
        assertThat(after.getContentAsString()).doesNotContain("internalNote").doesNotContain("ghi chu noi bo");

        assertThat(call(get("/api/sections/" + sectionId + "/my-grades"), studentIn, null).getStatus())
                .isEqualTo(200);
        assertThat(call(get(myGrade), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(get(myGrade), lecturerA, null).getStatus()).isEqualTo(403);
        assertThat(call(get("/api/sections/" + sectionId + "/my-grades"), studentOut, null).getStatus())
                .isEqualTo(403);

        // điểm đã công bố không sửa được cho đến khi hủy công bố
        assertThat(call(put(gradeUrl()), lecturerA, GRADE).getStatus()).isEqualTo(400);
        long gradeId = json(call(get(gradeUrl()), lecturerA, null)).path("id").asLong();
        assertThat(call(post("/api/grades/" + gradeId + "/unpublish"), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(post("/api/grades/" + gradeId + "/unpublish"), lecturerB, null).getStatus()).isEqualTo(403);
        assertThat(json(call(post("/api/grades/" + gradeId + "/unpublish"), lecturerA, null)).path("published")
                .asBoolean()).isFalse();
        assertThat(json(call(post("/api/grades/" + gradeId + "/publish"), admin, null)).path("published")
                .asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Grade xem điểm của lớp: chỉ giảng viên phụ trách/Admin xem điểm từng bài nộp, danh sách, điểm chính thức (có ghi chú nội bộ)")
    void staffViewsGrades() throws Exception {
        prepareSubmission();
        call(put(gradeUrl()), lecturerA, GRADE);
        String list = "/api/assignments/" + assignmentId + "/grades";
        String official = "/api/assignments/" + assignmentId + "/students/" + studentIn.id() + "/grade";

        for (String url : new String[]{gradeUrl(), list, official}) {
            assertThat(call(get(url), null, null).getStatus()).isEqualTo(401);
            assertThat(call(get(url), studentIn, null).getStatus()).isEqualTo(403);
            assertThat(call(get(url), lecturerB, null).getStatus()).isEqualTo(403);
            assertThat(call(get(url), lecturerA, null).getStatus()).isEqualTo(200);
            assertThat(call(get(url), admin, null).getStatus()).isEqualTo(200);
        }
        assertThat(json(call(get(list), lecturerA, null)).size()).isEqualTo(1);
        assertThat(json(call(get(gradeUrl()), lecturerA, null)).path("internalNote").asString())
                .isEqualTo("ghi chu noi bo");
    }

    @Test
    @DisplayName("Final grade: chỉ giảng viên phụ trách/Admin cấu hình trọng số, tính và công bố; sinh viên xem điểm tổng kết của mình sau khi công bố")
    void finalGradesFlow() throws Exception {
        prepareSubmission();
        call(put(gradeUrl()), lecturerA, GRADE);
        call(post("/api/assignments/" + assignmentId + "/grades/publish"), lecturerA, null);

        String weights = "/api/sections/" + sectionId + "/grade-weights";
        String body = "[{\"gradeType\":\"ASSIGNMENT\",\"weight\":100}]";
        assertThat(call(put(weights), null, body).getStatus()).isEqualTo(401);
        assertThat(call(put(weights), studentIn, body).getStatus()).isEqualTo(403);
        assertThat(call(put(weights), lecturerB, body).getStatus()).isEqualTo(403);
        assertThat(call(put(weights), lecturerA, "[{\"gradeType\":\"ASSIGNMENT\",\"weight\":50}]").getStatus())
                .isEqualTo(400);
        assertThat(call(put(weights), lecturerA, "[{\"weight\":100}]").getStatus()).isEqualTo(400);
        assertThat(call(put(weights), lecturerA, body).getStatus()).isEqualTo(200);
        assertThat(call(get(weights), studentIn, null).getStatus()).isEqualTo(200);
        assertThat(call(get(weights), studentOut, null).getStatus()).isEqualTo(403);

        String calculate = "/api/sections/" + sectionId + "/final-grades/calculate";
        assertThat(call(post(calculate), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(post(calculate), lecturerB, null).getStatus()).isEqualTo(403);
        MockHttpServletResponse calculated = call(post(calculate), lecturerA, null);
        assertThat(calculated.getStatus()).isEqualTo(200);
        JsonNode mine = null;
        for (JsonNode item : json(calculated)) {
            if (item.path("studentId").asLong() == studentIn.id()) {
                mine = item;
            }
        }
        assertThat(mine).isNotNull();
        assertThat(mine.path("score").asDouble()).isEqualTo(8.5);

        String myFinal = "/api/sections/" + sectionId + "/my-final-grade";
        assertThat(json(call(get(myFinal), studentIn, null)).path("published").asBoolean()).isFalse();

        String publish = "/api/sections/" + sectionId + "/final-grades/publish";
        assertThat(call(post(publish), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(post(publish), lecturerB, null).getStatus()).isEqualTo(403);
        assertThat(json(call(post(publish), lecturerA, null)).path("count").asInt()).isGreaterThanOrEqualTo(1);

        MockHttpServletResponse published = call(get(myFinal), studentIn, null);
        assertThat(json(published).path("published").asBoolean()).isTrue();
        assertThat(json(published).path("score").asDouble()).isEqualTo(8.5);
        assertThat(call(get(myFinal), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(get(myFinal), null, null).getStatus()).isEqualTo(401);

        String list = "/api/sections/" + sectionId + "/final-grades";
        assertThat(call(get(list), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(get(list), lecturerA, null).getStatus()).isEqualTo(200);
        assertThat(call(post(list + "/unpublish"), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(post(list + "/unpublish"), lecturerA, null).getStatus()).isEqualTo(200);
    }
}
