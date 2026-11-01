package com.lms.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class SubmissionControllerIntegrationTest extends ApiTestSupport {

    private String submissions(long assignmentId) {
        return "/api/assignments/" + assignmentId + "/submissions";
    }

    private long submitAs(Account student, long assignmentId, String note) throws Exception {
        MockHttpServletResponse r = upload(submissions(assignmentId), false, student, "bai.pdf", PDF,
                "content", note);
        assertThat(r.getStatus()).isEqualTo(201);
        return json(r).path("id").asLong();
    }

    @Test
    @DisplayName("Submission: sinh viên đã ghi danh nộp bài -> 201; danh tính theo JWT, studentId/late gửi lên bị bỏ qua; không lộ đường dẫn file")
    void studentSubmits() throws Exception {
        long assignmentId = createAssignment("");
        MockHttpServletResponse r = upload(submissions(assignmentId), false, studentIn, "bai.pdf", PDF,
                "content", "Ghi chu", "studentId", String.valueOf(studentOut.id()), "late", "true",
                "selectedForGrading", "true");
        assertThat(r.getStatus()).isEqualTo(201);
        JsonNode body = json(r);
        assertThat(body.path("studentId").asLong()).isEqualTo(studentIn.id());
        assertThat(body.path("attemptNumber").asInt()).isEqualTo(1);
        assertThat(body.path("late").asBoolean()).isFalse();
        assertThat(body.path("originalFileName").asString()).isEqualTo("bai.pdf");
        assertThat(r.getContentAsString()).doesNotContain("filePath").doesNotContain("assignments/");

        assertThat(json(upload(submissions(assignmentId), false, studentIn, "bai2.pdf", PDF)).path("attemptNumber")
                .asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("Submission nộp bài: chưa đăng nhập 401; giảng viên/Admin 403; sinh viên chưa ghi danh 403; file sai 400; bài tập không có 404")
    void submitRestrictions() throws Exception {
        long assignmentId = createAssignment("");
        assertThat(upload(submissions(assignmentId), false, null, "bai.pdf", PDF).getStatus()).isEqualTo(401);
        assertThat(upload(submissions(assignmentId), false, lecturerA, "bai.pdf", PDF).getStatus()).isEqualTo(403);
        assertThat(upload(submissions(assignmentId), false, admin, "bai.pdf", PDF).getStatus()).isEqualTo(403);
        assertThat(upload(submissions(assignmentId), false, studentOut, "bai.pdf", PDF).getStatus()).isEqualTo(403);
        assertThat(upload(submissions(assignmentId), false, studentIn, "bai.exe", PDF).getStatus()).isEqualTo(400);
        assertThat(upload(submissions(assignmentId), false, studentIn, "gia.pdf", "khong phai pdf".getBytes())
                .getStatus()).isEqualTo(400);
        assertThat(upload(submissions(999999999L), false, studentIn, "bai.pdf", PDF).getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("Submission: vượt số lần nộp cho phép -> 400")
    void maxSubmissionsEnforced() throws Exception {
        long assignmentId = createAssignment(",\"maxSubmissions\":1");
        submitAs(studentIn, assignmentId, "lan 1");
        assertThat(upload(submissions(assignmentId), false, studentIn, "bai.pdf", PDF).getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Submission xem/tải: chủ bài nộp và giảng viên phụ trách xem được; sinh viên khác, giảng viên lớp khác 403; tải về là attachment")
    void viewAndDownloadOwnership() throws Exception {
        long assignmentId = createAssignment("");
        long id = submitAs(studentIn, assignmentId, "bai cua toi");
        String url = "/api/submissions/" + id;

        assertThat(call(get(url), null, null).getStatus()).isEqualTo(401);
        assertThat(call(get(url), studentIn, null).getStatus()).isEqualTo(200);
        assertThat(call(get(url), lecturerA, null).getStatus()).isEqualTo(200);
        assertThat(call(get(url), admin, null).getStatus()).isEqualTo(200);
        assertThat(call(get(url), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(get(url), lecturerB, null).getStatus()).isEqualTo(403);
        assertThat(call(get("/api/submissions/999999999"), admin, null).getStatus()).isEqualTo(404);

        MockHttpServletResponse download = call(get(url + "/download"), studentIn, null);
        assertThat(download.getStatus()).isEqualTo(200);
        assertThat(download.getContentAsByteArray()).isEqualTo(PDF);
        assertThat(download.getHeader(HttpHeaders.CONTENT_DISPOSITION)).startsWith("attachment");
        assertThat(download.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(call(get(url + "/download"), lecturerA, null).getStatus()).isEqualTo(200);
        assertThat(call(get(url + "/download"), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(get(url + "/download"), lecturerB, null).getStatus()).isEqualTo(403);
        assertThat(call(get(url + "/download"), null, null).getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Submission danh sách/lịch sử: toàn bộ bài nộp chỉ cho người quản lý lớp; sinh viên chỉ xem lịch sử của chính mình")
    void listingAccess() throws Exception {
        long assignmentId = createAssignment("");
        submitAs(studentIn, assignmentId, "a");
        submitAs(studentIn, assignmentId, "b");

        assertThat(call(get(submissions(assignmentId)), null, null).getStatus()).isEqualTo(401);
        assertThat(call(get(submissions(assignmentId)), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(get(submissions(assignmentId)), lecturerB, null).getStatus()).isEqualTo(403);
        MockHttpServletResponse all = call(get(submissions(assignmentId)), lecturerA, null);
        assertThat(all.getStatus()).isEqualTo(200);
        assertThat(json(all).size()).isEqualTo(2);
        assertThat(call(get(submissions(assignmentId)), admin, null).getStatus()).isEqualTo(200);

        String history = "/api/assignments/" + assignmentId + "/students/" + studentIn.id() + "/submissions";
        assertThat(call(get(history), null, null).getStatus()).isEqualTo(401);
        assertThat(json(call(get(history), studentIn, null)).size()).isEqualTo(2);
        assertThat(call(get(history), lecturerA, null).getStatus()).isEqualTo(200);
        assertThat(call(get(history), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(get(history), lecturerB, null).getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("Submission chọn bài chấm: chỉ người quản lý lớp, và chỉ khi chính sách MANUAL; sinh viên 403")
    void selectForGradingAccess() throws Exception {
        long manual = createAssignment(",\"gradingPolicy\":\"MANUAL\"");
        long id = submitAs(studentIn, manual, "x");
        String select = "/api/submissions/" + id + "/select-for-grading";

        assertThat(call(put(select), null, null).getStatus()).isEqualTo(401);
        assertThat(call(put(select), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(put(select), lecturerB, null).getStatus()).isEqualTo(403);
        MockHttpServletResponse ok = call(put(select), lecturerA, null);
        assertThat(ok.getStatus()).isEqualTo(200);
        assertThat(json(ok).path("selectedForGrading").asBoolean()).isTrue();

        long latest = createAssignment("");
        long otherId = submitAs(studentIn, latest, "y");
        assertThat(call(put("/api/submissions/" + otherId + "/select-for-grading"), lecturerA, null).getStatus())
                .isEqualTo(400);

        String toGrade = "/api/assignments/" + manual + "/students/" + studentIn.id() + "/submission-to-grade";
        assertThat(call(get(toGrade), null, null).getStatus()).isEqualTo(401);
        assertThat(call(get(toGrade), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(get(toGrade), lecturerB, null).getStatus()).isEqualTo(403);
        assertThat(json(call(get(toGrade), lecturerA, null)).path("id").asLong()).isEqualTo(id);
    }
}
