package com.lms.controller;

import com.lms.entity.enums.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Tra cứu người dùng: Admin tìm theo từ khóa/vai trò/trạng thái có phân trang; giảng viên phụ trách chỉ tìm được
 * sinh viên chưa đăng ký lớp của mình qua API riêng; sinh viên không tra cứu được ai. Dữ liệu nhạy cảm không lộ ra.
 */
class UserSearchIntegrationTest extends ApiTestSupport {

    private String usernameOf(Account account) {
        return userRepository.findById(account.id()).orElseThrow().getUsername();
    }

    private List<Long> ids(JsonNode page) {
        List<Long> ids = new ArrayList<>();
        page.path("content").forEach(n -> ids.add(n.path("id").asLong()));
        return ids;
    }

    private MockHttpServletResponse adminSearch(Account who, String query) throws Exception {
        return call(get("/api/users" + query), who, null);
    }

    private MockHttpServletResponse candidates(Account who, long section, String query) throws Exception {
        return call(get("/api/sections/" + section + "/student-candidates" + query), who, null);
    }

    // ---------- Admin: GET /api/users ----------

    @Test
    @DisplayName("Admin: tìm theo từ khóa khớp một phần, không phân biệt hoa thường, trên username và email")
    void adminKeyword() throws Exception {
        String username = usernameOf(studentOut);

        MockHttpServletResponse byName = adminSearch(admin, "?keyword=" + username.toUpperCase().substring(0, 8));
        assertThat(byName.getStatus()).isEqualTo(200);
        assertThat(ids(json(byName))).contains(studentOut.id());

        MockHttpServletResponse byEmail = adminSearch(admin, "?keyword=" + username + "@EXAMPLE.com");
        assertThat(ids(json(byEmail))).containsExactly(studentOut.id());

        MockHttpServletResponse none = adminSearch(admin, "?keyword=khong-ton-tai-" + suffix());
        assertThat(json(none).path("content")).isEmpty();
    }

    @Test
    @DisplayName("Admin: lọc theo vai trò và trạng thái; chọn giảng viên đang hoạt động cho lớp")
    void adminRoleAndEnabledFilters() throws Exception {
        MockHttpServletResponse lecturers = adminSearch(admin, "?role=LECTURER&keyword=" + usernameOf(lecturerA));
        assertThat(ids(json(lecturers))).containsExactly(lecturerA.id());
        for (JsonNode user : json(lecturers).path("content")) {
            assertThat(user.path("roles").toString()).contains("LECTURER");
        }

        // Từ khóa của một giảng viên nhưng lọc vai trò STUDENT -> không có kết quả
        assertThat(json(adminSearch(admin, "?role=STUDENT&keyword=" + usernameOf(lecturerA))).path("content"))
                .isEmpty();

        assertThat(call(put("/api/users/" + lecturerB.id() + "/enabled"), admin, "{\"enabled\":false}").getStatus())
                .isEqualTo(200);
        String both = "?role=LECTURER&keyword=" + usernameOf(lecturerB);
        assertThat(ids(json(adminSearch(admin, both + "&enabled=true")))).isEmpty();
        assertThat(ids(json(adminSearch(admin, both + "&enabled=false")))).containsExactly(lecturerB.id());
        assertThat(ids(json(adminSearch(admin, both)))).containsExactly(lecturerB.id());
    }

    @Test
    @DisplayName("Admin: phân trang và sắp xếp hoạt động cùng bộ lọc")
    void adminPagination() throws Exception {
        // 5 tài khoản tạo sẵn trong test cùng tiền tố "t" nên tìm theo "t" luôn có >= 5 kết quả
        JsonNode first = json(adminSearch(admin, "?role=STUDENT&size=1&page=0&sort=id,asc"));
        JsonNode second = json(adminSearch(admin, "?role=STUDENT&size=1&page=1&sort=id,asc"));
        assertThat(first.path("content")).hasSize(1);
        assertThat(second.path("content")).hasSize(1);
        assertThat(first.path("totalElements").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(ids(first).get(0)).isLessThan(ids(second).get(0));

        assertThat(json(adminSearch(admin, "?size=5000")).path("content").size()).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Admin: ký tự đại diện của LIKE trong từ khóa bị coi là ký tự thường")
    void likeWildcardsAreEscaped() throws Exception {
        assertThat(json(adminSearch(admin, "?keyword=%")).path("content")).isEmpty();
        assertThat(json(adminSearch(admin, "?keyword=_")).path("content")).isEmpty();
        assertThat(json(adminSearch(admin, "?keyword=" + usernameOf(studentOut).substring(0, 3) + "%"))
                .path("content")).isEmpty();
    }

    @Test
    @DisplayName("Admin: giá trị role/sort không hợp lệ -> 400; không bao giờ lộ password")
    void invalidParamsAndNoSecrets() throws Exception {
        assertThat(adminSearch(admin, "?role=SUPERUSER").getStatus()).isEqualTo(400);
        assertThat(adminSearch(admin, "?sort=password,asc").getStatus()).isEqualTo(400);
        assertThat(adminSearch(admin, "?sort=roles.name,asc").getStatus()).isEqualTo(400);

        MockHttpServletResponse ok = adminSearch(admin, "?keyword=" + usernameOf(studentIn));
        String body = ok.getContentAsString(StandardCharsets.UTF_8).toLowerCase();
        assertThat(body).doesNotContain("password").doesNotContain("$2a$").doesNotContain("$2b$");
        JsonNode user = json(ok).path("content").get(0);
        assertThat(user.propertyNames()).doesNotContain("password");
    }

    @Test
    @DisplayName("Chỉ Admin dùng GET /api/users: giảng viên và sinh viên 403, chưa đăng nhập 401, kể cả khi giả role")
    void searchRequiresAdmin() throws Exception {
        for (Account who : List.of(lecturerA, studentIn)) {
            assertThat(adminSearch(who, "").getStatus()).isEqualTo(403);
            assertThat(adminSearch(who, "?role=STUDENT&keyword=a").getStatus()).isEqualTo(403);
            assertThat(call(get("/api/users").header("X-Role", "ADMIN").param("userId", String.valueOf(admin.id()))
                    .param("actorId", String.valueOf(admin.id())), who, null).getStatus()).isEqualTo(403);
        }
        assertThat(adminSearch(null, "").getStatus()).isEqualTo(401);
    }

    // ---------- Giảng viên phụ trách: GET /api/sections/{id}/student-candidates ----------

    @Test
    @DisplayName("Giảng viên phụ trách tìm được sinh viên chưa đăng ký; sinh viên đã ghi danh không xuất hiện")
    void managerFindsCandidates() throws Exception {
        MockHttpServletResponse found = candidates(lecturerA, sectionId, "?keyword=" + usernameOf(studentOut));
        assertThat(found.getStatus()).isEqualTo(200);
        assertThat(ids(json(found))).containsExactly(studentOut.id());

        // studentIn đã đăng ký lớp ở bước dựng dữ liệu
        assertThat(ids(json(candidates(lecturerA, sectionId, "?keyword=" + usernameOf(studentIn))))).isEmpty();

        // Admin cũng dùng được
        assertThat(ids(json(candidates(admin, sectionId, "?keyword=" + usernameOf(studentOut)))))
                .containsExactly(studentOut.id());
    }

    @Test
    @DisplayName("Ứng viên chỉ gồm id, username, họ tên — không email, điện thoại, vai trò, mật khẩu")
    void candidateFieldsAreMinimal() throws Exception {
        MockHttpServletResponse response = candidates(lecturerA, sectionId, "?keyword=" + usernameOf(studentOut));
        JsonNode student = json(response).path("content").get(0);
        List<String> fields = new ArrayList<>();
        student.propertyNames().forEach(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("id", "username", "fullName");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8).toLowerCase())
                .doesNotContain("password").doesNotContain("@example.com");
    }

    @Test
    @DisplayName("Ứng viên chỉ là sinh viên đang hoạt động: giảng viên, Admin, tài khoản bị khóa không lọt vào")
    void onlyEnabledStudents() throws Exception {
        assertThat(ids(json(candidates(lecturerA, sectionId, "?keyword=" + usernameOf(lecturerB))))).isEmpty();
        assertThat(ids(json(candidates(lecturerA, sectionId, "?keyword=" + usernameOf(admin))))).isEmpty();

        call(put("/api/users/" + studentOut.id() + "/enabled"), admin, "{\"enabled\":false}");
        assertThat(ids(json(candidates(lecturerA, sectionId, "?keyword=" + usernameOf(studentOut))))).isEmpty();
    }

    @Test
    @DisplayName("Client không đổi được phạm vi bằng role/userId/enabled giả")
    void clientCannotWidenScope() throws Exception {
        String forged = "&role=ADMIN&roles=ADMIN&userId=" + admin.id() + "&enabled=false&sectionId=0";
        MockHttpServletResponse response = candidates(lecturerA, sectionId, "?keyword=" + usernameOf(admin) + forged);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(json(response).path("content")).isEmpty();
    }

    @Test
    @DisplayName("Sinh viên đã rút khỏi lớp lại được tìm thấy để thêm lại")
    void droppedStudentIsCandidateAgain() throws Exception {
        String keyword = "?keyword=" + usernameOf(studentIn);
        assertThat(ids(json(candidates(lecturerA, sectionId, keyword)))).isEmpty();
        assertThat(call(post("/api/sections/" + sectionId + "/enrollments/" + studentIn.id() + "/withdraw"),
                lecturerA, null).getStatus()).isEqualTo(200);
        assertThat(ids(json(candidates(lecturerA, sectionId, keyword)))).containsExactly(studentIn.id());
    }

    @Test
    @DisplayName("Phân quyền: giảng viên không phụ trách, sinh viên 403; chưa đăng nhập 401; lớp không có 404")
    void candidatesAuthorization() throws Exception {
        String query = "?keyword=" + usernameOf(studentOut);
        assertThat(candidates(lecturerB, sectionId, query).getStatus()).isEqualTo(403);
        assertThat(candidates(studentOut, sectionId, query).getStatus()).isEqualTo(403);
        assertThat(candidates(studentIn, sectionId, query).getStatus()).isEqualTo(403);
        assertThat(candidates(null, sectionId, query).getStatus()).isEqualTo(401);
        assertThat(candidates(lecturerA, 999_999_999L, query).getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("Từ khóa bắt buộc và tối thiểu 2 ký tự: không thể liệt kê toàn bộ sinh viên")
    void keywordIsRequired() throws Exception {
        assertThat(candidates(lecturerA, sectionId, "").getStatus()).isEqualTo(400);
        assertThat(candidates(lecturerA, sectionId, "?keyword=").getStatus()).isEqualTo(400);
        assertThat(candidates(lecturerA, sectionId, "?keyword=   ").getStatus()).isEqualTo(400);
        assertThat(candidates(lecturerA, sectionId, "?keyword=a").getStatus()).isEqualTo(400);
        assertThat(candidates(lecturerA, sectionId, "?keyword=ab").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Ứng viên: phân trang giới hạn 100, sort ngoài danh sách bị từ chối")
    void candidatePaginationAndSort() throws Exception {
        assertThat(candidates(lecturerA, sectionId, "?keyword=ab&sort=email,asc").getStatus()).isEqualTo(400);
        assertThat(candidates(lecturerA, sectionId, "?keyword=ab&sort=password,asc").getStatus()).isEqualTo(400);
        assertThat(candidates(lecturerA, sectionId, "?keyword=ab&sort=username,asc").getStatus()).isEqualTo(200);

        JsonNode page = json(candidates(lecturerA, sectionId, "?keyword=ab&size=5000"));
        assertThat(page.path("content").size()).isLessThanOrEqualTo(100);
    }

    // ---------- Tích hợp với thao tác lớp học phần ----------

    @Test
    @DisplayName("Kết quả tra cứu dùng trực tiếp được cho thêm sinh viên và gán giảng viên")
    void searchResultsFeedSectionOperations() throws Exception {
        long candidate = ids(json(candidates(lecturerA, sectionId, "?keyword=" + usernameOf(studentOut)))).get(0);
        assertThat(call(post("/api/sections/" + sectionId + "/enrollments"), lecturerA,
                "{\"studentId\":" + candidate + "}").getStatus()).isEqualTo(201);
        assertThat(ids(json(candidates(lecturerA, sectionId, "?keyword=" + usernameOf(studentOut))))).isEmpty();

        long teacher = ids(json(adminSearch(admin, "?role=LECTURER&enabled=true&keyword=" + usernameOf(lecturerB))))
                .get(0);
        assertThat(call(put("/api/sections/" + sectionId + "/teacher"), admin, "{\"teacherId\":" + teacher + "}")
                .getStatus()).isEqualTo(200);

        // Một sinh viên không thể được gán làm giảng viên dù có id (quy tắc cũ vẫn giữ nguyên)
        assertThat(call(put("/api/sections/" + sectionId + "/teacher"), admin,
                "{\"teacherId\":" + studentIn.id() + "}").getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Quy tắc cũ không đổi: sinh viên vẫn bị 403 trên GET /api/users, vai trò lấy từ DB không từ client")
    void rolesComeFromServer() throws Exception {
        assertThat(userRepository.findById(studentIn.id()).orElseThrow().getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.STUDENT)).isTrue();
        assertThat(adminSearch(studentIn, "?role=STUDENT").getStatus()).isEqualTo(403);
    }
}
