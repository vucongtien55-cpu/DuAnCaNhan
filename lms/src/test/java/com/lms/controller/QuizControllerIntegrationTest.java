package com.lms.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class QuizControllerIntegrationTest extends ApiTestSupport {

    private static final String QUESTIONS = "[{\"content\":\"1+1=?\",\"score\":5,\"explanation\":\"Cong co ban\","
            + "\"options\":[{\"content\":\"2\",\"correct\":true},{\"content\":\"3\",\"correct\":false}]},"
            + "{\"content\":\"2+2=?\",\"score\":5,\"options\":[{\"content\":\"4\",\"correct\":true},"
            + "{\"content\":\"5\",\"correct\":false}]}]";

    private long quizId;

    private String quizBody(String questions) {
        return "{\"sectionId\":" + sectionId + ",\"title\":\"Kiem tra 1\",\"durationMinutes\":30,\"maxAttempts\":2"
                + (questions == null ? "" : ",\"questions\":" + questions) + "}";
    }

    private void createQuiz(boolean publish) throws Exception {
        MockHttpServletResponse r = call(post("/api/quizzes"), lecturerA, quizBody(QUESTIONS));
        assertThat(r.getStatus()).as(r.getContentAsString()).isEqualTo(201);
        quizId = json(r).path("id").asLong();
        if (publish) {
            assertThat(call(post("/api/quizzes/" + quizId + "/publish"), lecturerA, null).getStatus())
                    .isEqualTo(200);
        }
    }

    @Test
    @DisplayName("Quiz tạo/sửa/xóa: giảng viên phụ trách và Admin được; chưa đăng nhập 401; sinh viên và giảng viên lớp khác 403; dữ liệu sai 400")
    void managementAccess() throws Exception {
        assertThat(call(post("/api/quizzes"), null, quizBody(QUESTIONS)).getStatus()).isEqualTo(401);
        assertThat(call(post("/api/quizzes"), studentIn, quizBody(QUESTIONS)).getStatus()).isEqualTo(403);
        assertThat(call(post("/api/quizzes"), lecturerB, quizBody(QUESTIONS)).getStatus()).isEqualTo(403);
        assertThat(call(post("/api/quizzes"), lecturerA, "{\"sectionId\":" + sectionId + "}").getStatus())
                .isEqualTo(400);
        assertThat(call(post("/api/quizzes"), admin, quizBody(null)).getStatus()).isEqualTo(201);

        createQuiz(false);
        String url = "/api/quizzes/" + quizId;
        String update = "{\"title\":\"Sua\",\"durationMinutes\":45,\"maxAttempts\":1,\"shuffleQuestions\":true}";
        assertThat(call(put(url), studentIn, update).getStatus()).isEqualTo(403);
        assertThat(call(put(url), lecturerB, update).getStatus()).isEqualTo(403);
        assertThat(json(call(put(url), lecturerA, update)).path("durationMinutes").asInt()).isEqualTo(45);

        assertThat(call(put(url + "/questions"), studentIn, QUESTIONS).getStatus()).isEqualTo(403);
        assertThat(call(put(url + "/questions"), lecturerB, QUESTIONS).getStatus()).isEqualTo(403);
        assertThat(call(put(url + "/questions"), lecturerA, "[{\"content\":\"\"}]").getStatus()).isEqualTo(400);
        assertThat(call(put(url + "/questions"), lecturerA, QUESTIONS).getStatus()).isEqualTo(200);

        assertThat(call(post(url + "/publish"), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(post(url + "/publish"), lecturerB, null).getStatus()).isEqualTo(403);
        assertThat(call(delete(url), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(delete(url), lecturerB, null).getStatus()).isEqualTo(403);
        assertThat(call(delete(url), lecturerA, null).getStatus()).isEqualTo(204);
        assertThat(call(get(url), admin, null).getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("Quiz: chỉ người quản lý xem được đáp án đúng; sinh viên 403; danh sách lớp cho sinh viên chỉ có quiz đã công bố, không có câu hỏi")
    void answersVisibleToManagersOnly() throws Exception {
        createQuiz(false);
        String url = "/api/quizzes/" + quizId;
        assertThat(call(get(url), null, null).getStatus()).isEqualTo(401);
        assertThat(call(get(url), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(get(url), lecturerB, null).getStatus()).isEqualTo(403);
        MockHttpServletResponse detail = call(get(url), lecturerA, null);
        assertThat(detail.getContentAsString()).contains("\"correct\":true");

        String list = "/api/sections/" + sectionId + "/quizzes";
        assertThat(call(get(list), null, null).getStatus()).isEqualTo(401);
        assertThat(call(get(list), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(get(list), lecturerB, null).getStatus()).isEqualTo(403);
        assertThat(json(call(get(list), lecturerA, null)).size()).isEqualTo(1);
        assertThat(json(call(get(list), studentIn, null)).size()).isEqualTo(0);

        call(post(url + "/publish"), lecturerA, null);
        MockHttpServletResponse visible = call(get(list), studentIn, null);
        assertThat(json(visible).size()).isEqualTo(1);
        assertThat(visible.getContentAsString()).doesNotContain("correct").doesNotContain("questions\"");
    }

    @Test
    @DisplayName("Quiz làm bài: sinh viên ghi danh bắt đầu, lưu đáp án, nộp; đề không chứa đáp án đúng; chưa công bố thì không thấy điểm")
    void studentTakesQuiz() throws Exception {
        createQuiz(true);
        String start = "/api/quizzes/" + quizId + "/attempts";
        assertThat(call(post(start), null, null).getStatus()).isEqualTo(401);
        assertThat(call(post(start), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(post(start), lecturerA, null).getStatus()).isEqualTo(403);
        assertThat(call(post(start), admin, null).getStatus()).isEqualTo(403);

        MockHttpServletResponse started = call(post(start), studentIn, null);
        assertThat(started.getStatus()).isEqualTo(200);
        assertThat(started.getContentAsString()).doesNotContain("correct").doesNotContain("explanation")
                .doesNotContain("Cong co ban");
        JsonNode attempt = json(started);
        long attemptId = attempt.path("attemptId").asLong();
        assertThat(attempt.path("remainingSeconds").asLong()).isGreaterThan(0);
        JsonNode question = attempt.path("questions").get(0);
        long questionId = question.path("id").asLong();
        long optionId = question.path("options").get(0).path("id").asLong();

        String answers = "/api/quiz-attempts/" + attemptId + "/answers";
        String answer = "{\"questionId\":" + questionId + ",\"selectedOptionId\":" + optionId + "}";
        assertThat(call(put(answers), null, answer).getStatus()).isEqualTo(401);
        assertThat(call(put(answers), studentOut, answer).getStatus()).isEqualTo(403);
        assertThat(call(put(answers), lecturerA, answer).getStatus()).isEqualTo(403);
        assertThat(call(put(answers), studentIn, "{}").getStatus()).isEqualTo(400);
        assertThat(call(put(answers), studentIn, "{\"questionId\":" + questionId + ",\"selectedOptionId\":999999999}")
                .getStatus()).isEqualTo(400);
        assertThat(call(put(answers), studentIn, answer).getStatus()).isEqualTo(200);
        assertThat(call(get("/api/quiz-attempts/" + attemptId), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(json(call(get("/api/quiz-attempts/" + attemptId), studentIn, null)).path("savedAnswers")
                .size()).isEqualTo(1);

        String submit = "/api/quiz-attempts/" + attemptId + "/submit";
        assertThat(call(post(submit), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(post(submit), null, null).getStatus()).isEqualTo(401);
        MockHttpServletResponse result = call(post(submit), studentIn, null);
        assertThat(result.getStatus()).isEqualTo(200);
        assertThat(json(result).path("gradePublished").asBoolean()).isFalse();
        assertThat(json(result).path("score").isNull()).isTrue();
        // nộp lại là an toàn; sửa đáp án sau khi nộp bị từ chối
        assertThat(call(post(submit), studentIn, null).getStatus()).isEqualTo(200);
        assertThat(call(put(answers), studentIn, answer).getStatus()).isEqualTo(400);

        assertThat(call(get("/api/quiz-attempts/" + attemptId + "/result"), studentOut, null).getStatus())
                .isEqualTo(403);
        assertThat(json(call(get("/api/quizzes/" + quizId + "/my-attempts"), studentIn, null)).size())
                .isEqualTo(1);
        assertThat(json(call(get("/api/quizzes/" + quizId + "/my-attempts"), studentOut, null)).size())
                .isEqualTo(0);
    }

    @Test
    @DisplayName("Quiz điểm: giảng viên phụ trách xem lượt làm và công bố điểm; sinh viên chỉ thấy điểm sau khi công bố; vai trò khác 403")
    void staffViewsAttemptsAndPublishes() throws Exception {
        createQuiz(true);
        JsonNode attempt = json(call(post("/api/quizzes/" + quizId + "/attempts"), studentIn, null));
        long attemptId = attempt.path("attemptId").asLong();
        for (JsonNode q : attempt.path("questions")) {
            // chọn lựa chọn đầu tiên của mỗi câu (đều là đáp án đúng trong dữ liệu thử)
            call(put("/api/quiz-attempts/" + attemptId + "/answers"), studentIn, "{\"questionId\":"
                    + q.path("id").asLong() + ",\"selectedOptionId\":" + q.path("options").get(0).path("id").asLong()
                    + "}");
        }
        call(post("/api/quiz-attempts/" + attemptId + "/submit"), studentIn, null);

        String attempts = "/api/quizzes/" + quizId + "/attempts";
        assertThat(call(get(attempts), null, null).getStatus()).isEqualTo(401);
        assertThat(call(get(attempts), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(get(attempts), lecturerB, null).getStatus()).isEqualTo(403);
        JsonNode staffView = json(call(get(attempts), lecturerA, null));
        assertThat(staffView.size()).isEqualTo(1);
        assertThat(staffView.get(0).path("studentId").asLong()).isEqualTo(studentIn.id());
        assertThat(staffView.get(0).path("score").asDouble()).isEqualTo(10.0);
        assertThat(staffView.get(0).path("gradePublished").asBoolean()).isFalse();

        String publish = "/api/quizzes/" + quizId + "/grades/publish";
        assertThat(call(post(publish), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(post(publish), lecturerB, null).getStatus()).isEqualTo(403);
        assertThat(json(call(post(publish), lecturerA, null)).path("count").asInt()).isEqualTo(1);

        MockHttpServletResponse result = call(get("/api/quiz-attempts/" + attemptId + "/result"), studentIn, null);
        assertThat(json(result).path("gradePublished").asBoolean()).isTrue();
        assertThat(json(result).path("score").asDouble()).isEqualTo(10.0);
        assertThat(result.getContentAsString()).doesNotContain("correct");
    }
}
