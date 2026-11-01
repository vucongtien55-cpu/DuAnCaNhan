package com.lms.controller;

import com.lms.dto.user.CreateUserRequest;
import com.lms.entity.User;
import com.lms.entity.enums.RoleName;
import com.lms.repository.UserRepository;
import com.lms.security.JwtTokenService;
import com.lms.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Kiểm thử HTTP cho UserController và CourseController: truy cập hợp lệ, chưa đăng nhập, sai quyền, và dữ liệu nhạy cảm
 * không lọt vào response. Dữ liệu nằm trong giao dịch được hoàn tác nên không để lại gì trong database.
 */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
@TestPropertySource(properties = {"app.admin.username=", "app.admin.password="})
class UserCourseControllerIntegrationTest {

    private static final String PASSWORD = "Password123";

    @Autowired MockMvc mockMvc;
    @Autowired JsonMapper jsonMapper;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired JwtTokenService tokenService;

    // ---------- helpers ----------

    private record Account(Long id, String token) {
    }

    private Account account(RoleName role) {
        String name = "t" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Long id = userService.createUser(new CreateUserRequest(name, name + "@example.com", PASSWORD,
                "Nguoi dung " + name, null, Set.of(role))).id();
        User user = userRepository.findById(id).orElseThrow();
        return new Account(id, tokenService.issue(user).value());
    }

    private MockHttpServletResponse call(MockHttpServletRequestBuilder request, String token, String body)
            throws Exception {
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request).andReturn().getResponse();
    }

    private JsonNode json(MockHttpServletResponse response) throws Exception {
        return jsonMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    }

    private String courseBody(String code, String status) {
        return "{\"code\":\"" + code + "\",\"title\":\"Lap trinh Java\",\"credits\":3"
                + (status == null ? "" : ",\"status\":\"" + status + "\"") + "}";
    }

    private String newCode() {
        return "C" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private long createCourse(Account admin, String code, String status) throws Exception {
        MockHttpServletResponse created = call(post("/api/courses"), admin.token(), courseBody(code, null));
        assertThat(created.getStatus()).isEqualTo(201);
        long id = json(created).path("id").asLong();
        if (status != null) {
            MockHttpServletResponse updated =
                    call(put("/api/courses/" + id), admin.token(), courseBody(code, status));
            assertThat(updated.getStatus()).isEqualTo(200);
        }
        return id;
    }

    private String createUserBody(String name) {
        return "{\"username\":\"" + name + "\",\"email\":\"" + name + "@example.com\",\"password\":\"" + PASSWORD
                + "\",\"fullName\":\"Ten day du\",\"roles\":[\"STUDENT\"]}";
    }

    // ---------- UserController ----------

    @Test
    @DisplayName("User: chưa đăng nhập hoặc token rác -> 401 trên mọi endpoint")
    void usersRequireAuthentication() throws Exception {
        assertThat(call(get("/api/users"), null, null).getStatus()).isEqualTo(401);
        assertThat(call(get("/api/users/me"), null, null).getStatus()).isEqualTo(401);
        assertThat(call(put("/api/users/me"), null,
                "{\"email\":\"a@b.com\",\"fullName\":\"A\"}").getStatus()).isEqualTo(401);
        assertThat(call(post("/api/users"), null, createUserBody("khongtoken")).getStatus()).isEqualTo(401);
        assertThat(call(put("/api/users/1/enabled"), null, "{\"enabled\":false}").getStatus()).isEqualTo(401);
        assertThat(call(get("/api/users"), "token.rac.hoan.toan", null).getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("User: Student/Lecturer không được tạo, liệt kê, xem, sửa, khóa, đổi vai trò, đặt lại mật khẩu -> 403")
    void nonAdminCannotManageOthers() throws Exception {
        Account victim = account(RoleName.STUDENT);
        for (RoleName role : new RoleName[]{RoleName.STUDENT, RoleName.LECTURER}) {
            String token = account(role).token();
            String base = "/api/users/" + victim.id();
            assertThat(call(post("/api/users"), token, createUserBody("hack" + role)).getStatus()).isEqualTo(403);
            assertThat(call(get("/api/users"), token, null).getStatus()).isEqualTo(403);
            assertThat(call(get(base), token, null).getStatus()).isEqualTo(403);
            assertThat(call(put(base), token, "{\"email\":\"x@y.com\",\"fullName\":\"X\"}").getStatus())
                    .isEqualTo(403);
            assertThat(call(put(base + "/enabled"), token, "{\"enabled\":false}").getStatus()).isEqualTo(403);
            assertThat(call(put(base + "/roles"), token, "{\"roles\":[\"ADMIN\"]}").getStatus()).isEqualTo(403);
            assertThat(call(put(base + "/password"), token, "{\"newPassword\":\"MatKhauMoi123\"}").getStatus())
                    .isEqualTo(403);
        }
        User unchanged = userRepository.findById(victim.id()).orElseThrow();
        assertThat(unchanged.isEnabled()).isTrue();
        assertThat(unchanged.getRoles()).extracting(r -> r.getName()).containsExactly(RoleName.STUDENT);
    }

    @Test
    @DisplayName("User: Admin tạo, liệt kê, xem, khóa, mở khóa; response không chứa password")
    void adminManagesUsers() throws Exception {
        Account admin = account(RoleName.ADMIN);
        String name = "nv" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        MockHttpServletResponse created = call(post("/api/users"), admin.token(), createUserBody(name));
        assertThat(created.getStatus()).isEqualTo(201);
        long id = json(created).path("id").asLong();
        String raw = created.getContentAsString(StandardCharsets.UTF_8);
        assertThat(raw).doesNotContainIgnoringCase("password").doesNotContain(PASSWORD).doesNotContain("$2");

        MockHttpServletResponse fetched = call(get("/api/users/" + id), admin.token(), null);
        assertThat(fetched.getStatus()).isEqualTo(200);
        assertThat(fetched.getContentAsString(StandardCharsets.UTF_8)).doesNotContainIgnoringCase("password");

        MockHttpServletResponse list = call(get("/api/users?size=5"), admin.token(), null);
        assertThat(list.getStatus()).isEqualTo(200);
        assertThat(list.getContentAsString(StandardCharsets.UTF_8)).doesNotContainIgnoringCase("password");

        MockHttpServletResponse locked = call(put("/api/users/" + id + "/enabled"), admin.token(),
                "{\"enabled\":false}");
        assertThat(locked.getStatus()).isEqualTo(200);
        assertThat(json(locked).path("enabled").asBoolean()).isFalse();

        MockHttpServletResponse unlocked = call(put("/api/users/" + id + "/enabled"), admin.token(),
                "{\"enabled\":true}");
        assertThat(unlocked.getStatus()).isEqualTo(200);
        assertThat(json(unlocked).path("enabled").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("User: tài khoản bị khóa mất quyền ngay dù token còn hạn")
    void lockedUserTokenRejected() throws Exception {
        Account admin = account(RoleName.ADMIN);
        Account student = account(RoleName.STUDENT);
        assertThat(call(get("/api/users/me"), student.token(), null).getStatus()).isEqualTo(200);

        call(put("/api/users/" + student.id() + "/enabled"), admin.token(), "{\"enabled\":false}");

        assertThat(call(get("/api/users/me"), student.token(), null).getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("User: validate đầu vào -> 400, không tồn tại -> 404, trùng username -> 400 (mã lỗi thống nhất)")
    void adminErrorHandling() throws Exception {
        Account admin = account(RoleName.ADMIN);
        assertThat(call(post("/api/users"), admin.token(), "{\"username\":\"a\"}").getStatus()).isEqualTo(400);
        assertThat(call(get("/api/users/999999999"), admin.token(), null).getStatus()).isEqualTo(404);
        assertThat(call(put("/api/users/" + admin.id() + "/enabled"), admin.token(), "{}").getStatus())
                .isEqualTo(400);

        String name = "dup" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        assertThat(call(post("/api/users"), admin.token(), createUserBody(name)).getStatus()).isEqualTo(201);
        MockHttpServletResponse dup = call(post("/api/users"), admin.token(), createUserBody(name));
        assertThat(dup.getStatus()).isEqualTo(400);
        assertThat(json(dup).path("code").asString()).isEqualTo("BUSINESS_RULE_VIOLATION");
    }

    @Test
    @DisplayName("User: không được khóa hoặc gỡ quyền Admin cuối cùng của hệ thống")
    void cannotRemoveLastAdmin() throws Exception {
        // Chỉ giữ lại một Admin hoạt động để kiểm tra quy tắc (giao dịch được hoàn tác sau test).
        Account admin = account(RoleName.ADMIN);
        userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(admin.id()))
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN))
                .forEach(u -> u.setEnabled(false));
        userRepository.flush();

        assertThat(call(put("/api/users/" + admin.id() + "/enabled"), admin.token(), "{\"enabled\":false}")
                .getStatus()).isEqualTo(400);
        assertThat(call(put("/api/users/" + admin.id() + "/roles"), admin.token(), "{\"roles\":[\"STUDENT\"]}")
                .getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("User: người dùng xem/sửa hồ sơ của chính mình theo JWT; id/role/enabled client gửi lên bị bỏ qua")
    void selfServiceUsesJwtIdentity() throws Exception {
        Account me = account(RoleName.STUDENT);
        Account other = account(RoleName.STUDENT);

        MockHttpServletResponse profile = call(get("/api/users/me"), me.token(), null);
        assertThat(profile.getStatus()).isEqualTo(200);
        assertThat(json(profile).path("id").asLong()).isEqualTo(me.id());
        assertThat(profile.getContentAsString(StandardCharsets.UTF_8)).doesNotContainIgnoringCase("password");

        String otherEmailBefore = userRepository.findById(other.id()).orElseThrow().getEmail();
        String body = "{\"id\":" + other.id() + ",\"userId\":" + other.id() + ",\"actorId\":" + other.id()
                + ",\"roles\":[\"ADMIN\"],\"enabled\":false,\"password\":\"HackPassword1\","
                + "\"email\":\"moi" + me.id() + "@example.com\",\"fullName\":\"Ten Moi\",\"phone\":\"0900000000\"}";
        MockHttpServletResponse updated = call(put("/api/users/me"), me.token(), body);
        assertThat(updated.getStatus()).isEqualTo(200);
        assertThat(json(updated).path("id").asLong()).isEqualTo(me.id());
        assertThat(json(updated).path("fullName").asString()).isEqualTo("Ten Moi");

        User mine = userRepository.findById(me.id()).orElseThrow();
        assertThat(mine.getRoles()).extracting(r -> r.getName()).containsExactly(RoleName.STUDENT);
        assertThat(mine.isEnabled()).isTrue();
        assertThat(userRepository.findById(other.id()).orElseThrow().getEmail()).isEqualTo(otherEmailBefore);
    }

    @Test
    @DisplayName("User: đổi mật khẩu của mình cần đúng mật khẩu cũ; sai -> 400, thành công -> 204 không có body")
    void changeOwnPassword() throws Exception {
        Account me = account(RoleName.STUDENT);
        assertThat(call(put("/api/users/me/password"), null,
                "{\"oldPassword\":\"" + PASSWORD + "\",\"newPassword\":\"MatKhauMoi123\"}").getStatus())
                .isEqualTo(401);
        assertThat(call(put("/api/users/me/password"), me.token(),
                "{\"oldPassword\":\"SaiMatKhau99\",\"newPassword\":\"MatKhauMoi123\"}").getStatus()).isEqualTo(400);

        MockHttpServletResponse ok = call(put("/api/users/me/password"), me.token(),
                "{\"oldPassword\":\"" + PASSWORD + "\",\"newPassword\":\"MatKhauMoi123\"}");
        assertThat(ok.getStatus()).isEqualTo(204);
        assertThat(ok.getContentAsString()).isEmpty();
    }

    // ---------- CourseController ----------

    @Test
    @DisplayName("Course: chưa đăng nhập -> 401 trên mọi endpoint")
    void coursesRequireAuthentication() throws Exception {
        assertThat(call(get("/api/courses"), null, null).getStatus()).isEqualTo(401);
        assertThat(call(get("/api/courses/1"), null, null).getStatus()).isEqualTo(401);
        assertThat(call(post("/api/courses"), null, courseBody("X1", null)).getStatus()).isEqualTo(401);
        assertThat(call(put("/api/courses/1"), null, courseBody("X1", "ACTIVE")).getStatus()).isEqualTo(401);
        assertThat(call(delete("/api/courses/1"), null, null).getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Course: Student/Lecturer không được tạo, sửa, xóa -> 403 và dữ liệu không đổi")
    void nonAdminCannotWriteCourses() throws Exception {
        Account admin = account(RoleName.ADMIN);
        String code = newCode();
        long id = createCourse(admin, code, "ACTIVE");

        for (RoleName role : new RoleName[]{RoleName.STUDENT, RoleName.LECTURER}) {
            String token = account(role).token();
            assertThat(call(post("/api/courses"), token, courseBody(newCode(), null)).getStatus()).isEqualTo(403);
            assertThat(call(put("/api/courses/" + id), token, courseBody(code, "ARCHIVED")).getStatus())
                    .isEqualTo(403);
            assertThat(call(delete("/api/courses/" + id), token, null).getStatus()).isEqualTo(403);
        }
        MockHttpServletResponse still = call(get("/api/courses/" + id), admin.token(), null);
        assertThat(json(still).path("status").asString()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Course: Admin tạo (DRAFT), xem, sửa, lọc theo trạng thái, xóa")
    void adminManagesCourses() throws Exception {
        Account admin = account(RoleName.ADMIN);
        String code = newCode();

        MockHttpServletResponse created = call(post("/api/courses"), admin.token(), courseBody(code, null));
        assertThat(created.getStatus()).isEqualTo(201);
        assertThat(json(created).path("status").asString()).isEqualTo("DRAFT");
        long id = json(created).path("id").asLong();

        assertThat(call(get("/api/courses/" + id), admin.token(), null).getStatus()).isEqualTo(200);
        assertThat(call(get("/api/courses?status=DRAFT&size=5"), admin.token(), null).getStatus()).isEqualTo(200);

        MockHttpServletResponse updated = call(put("/api/courses/" + id), admin.token(), courseBody(code, "ACTIVE"));
        assertThat(updated.getStatus()).isEqualTo(200);
        assertThat(json(updated).path("status").asString()).isEqualTo("ACTIVE");

        assertThat(call(delete("/api/courses/" + id), admin.token(), null).getStatus()).isEqualTo(204);
        assertThat(call(get("/api/courses/" + id), admin.token(), null).getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("Course: người không phải Admin chỉ thấy khóa học ACTIVE; DRAFT/ARCHIVED -> 404, lọc trạng thái khác -> 403")
    void nonAdminSeesOnlyActiveCourses() throws Exception {
        Account admin = account(RoleName.ADMIN);
        Account student = account(RoleName.STUDENT);
        long active = createCourse(admin, newCode(), "ACTIVE");
        long draft = createCourse(admin, newCode(), null);
        long archived = createCourse(admin, newCode(), "ARCHIVED");

        assertThat(call(get("/api/courses/" + active), student.token(), null).getStatus()).isEqualTo(200);
        assertThat(call(get("/api/courses/" + draft), student.token(), null).getStatus()).isEqualTo(404);
        assertThat(call(get("/api/courses/" + archived), student.token(), null).getStatus()).isEqualTo(404);
        assertThat(call(get("/api/courses?status=DRAFT"), student.token(), null).getStatus()).isEqualTo(403);

        MockHttpServletResponse list = call(get("/api/courses?size=200"), student.token(), null);
        assertThat(list.getStatus()).isEqualTo(200);
        for (JsonNode course : json(list).path("content")) {
            assertThat(course.path("status").asString()).isEqualTo("ACTIVE");
        }
    }

    @Test
    @DisplayName("Course: validate đầu vào -> 400, trùng mã -> 400, không tồn tại -> 404, id sai kiểu -> 400")
    void courseErrorHandling() throws Exception {
        Account admin = account(RoleName.ADMIN);
        String code = newCode();
        assertThat(call(post("/api/courses"), admin.token(), "{\"code\":\"\",\"title\":\"\"}").getStatus())
                .isEqualTo(400);
        assertThat(call(put("/api/courses/1"), admin.token(), courseBody(code, null)).getStatus()).isEqualTo(400);
        createCourse(admin, code, null);
        MockHttpServletResponse dup = call(post("/api/courses"), admin.token(), courseBody(code, null));
        assertThat(dup.getStatus()).isEqualTo(400);
        assertThat(json(dup).path("code").asString()).isEqualTo("BUSINESS_RULE_VIOLATION");
        assertThat(call(get("/api/courses/999999999"), admin.token(), null).getStatus()).isEqualTo(404);
        assertThat(call(delete("/api/courses/999999999"), admin.token(), null).getStatus()).isEqualTo(404);
        assertThat(call(get("/api/courses/abc"), admin.token(), null).getStatus()).isEqualTo(400);
    }
}
