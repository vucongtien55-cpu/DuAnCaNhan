package com.lms.controller;

import com.lms.entity.enums.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = {
        "app.rate-limit.write.burst=5", "app.rate-limit.write.per-minute=1",
        "app.rate-limit.upload.burst=2", "app.rate-limit.upload.per-minute=1",
        "app.rate-limit.login.max-failures-per-account=3", "app.rate-limit.login.max-failures-per-ip=8"})
class RateLimitIntegrationTest extends ApiTestSupport {

    private static final String PROFILE = "{\"email\":\"a@example.com\",\"fullName\":\"Ten\"}";

    private MockHttpServletResponse login(String username, String password, String ip) throws Exception {
        var request = post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        request.with(r -> {
            r.setRemoteAddr(ip);
            return r;
        });
        return mockMvc.perform(request).andReturn().getResponse();
    }

    private String usernameOf(Account account) {
        return userRepository.findById(account.id()).orElseThrow().getUsername();
    }

    // ---------- request ghi ----------

    @Test
    @DisplayName("Ghi: vượt burst -> 429 TOO_MANY_REQUESTS kèm Retry-After và ApiError; request đọc không bị đếm")
    void writesAreLimitedPerUser() throws Exception {
        Account user = account(RoleName.STUDENT);
        for (int i = 0; i < 5; i++) {
            String body = "{\"email\":\"u" + i + user.id() + "@example.com\",\"fullName\":\"Ten\"}";
            assertThat(call(put("/api/users/me"), user, body).getStatus()).as("lần " + i).isEqualTo(200);
        }
        MockHttpServletResponse limited = call(put("/api/users/me"), user, PROFILE);
        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(Long.parseLong(limited.getHeader("Retry-After"))).isPositive();
        JsonNode body = json(limited);
        assertThat(body.path("status").asInt()).isEqualTo(429);
        assertThat(body.path("code").asString()).isEqualTo("TOO_MANY_REQUESTS");

        // đọc dữ liệu vẫn bình thường
        assertThat(call(get("/api/users/me"), user, null).getStatus()).isEqualTo(200);
        assertThat(call(get("/api/courses"), user, null).getStatus()).isEqualTo(200);
        // ghi ở endpoint khác cũng bị giới hạn chung theo người dùng
        assertThat(call(post("/api/auth/logout"), user, null).getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("Ghi: giới hạn tính theo người dùng — người khác không bị ảnh hưởng; request chưa đăng nhập vẫn là 401, không phải 429")
    void limitIsPerUser() throws Exception {
        Account heavy = account(RoleName.STUDENT);
        Account other = account(RoleName.STUDENT);
        for (int i = 0; i < 6; i++) {
            call(put("/api/users/me"), heavy, PROFILE);
        }
        assertThat(call(put("/api/users/me"), heavy, PROFILE).getStatus()).isEqualTo(429);
        assertThat(call(put("/api/users/me"), other, "{\"email\":\"o" + other.id()
                + "@example.com\",\"fullName\":\"Ten\"}").getStatus()).isEqualTo(200);
        for (int i = 0; i < 10; i++) {
            assertThat(call(put("/api/users/me"), null, PROFILE).getStatus()).isEqualTo(401);
        }
    }

    @Test
    @DisplayName("Ghi: danh tính tính theo JWT — header/tham số userId giả của client không đổi được bucket")
    void clientCannotChooseBucket() throws Exception {
        Account heavy = account(RoleName.STUDENT);
        Account victim = account(RoleName.STUDENT);
        for (int i = 0; i < 5; i++) {
            call(put("/api/users/me?userId=" + victim.id()).header("X-User-Id", String.valueOf(victim.id())),
                    heavy, PROFILE);
        }
        assertThat(call(put("/api/users/me").header("X-User-Id", String.valueOf(victim.id())), heavy, PROFILE)
                .getStatus()).isEqualTo(429);
        assertThat(call(put("/api/users/me"), victim, "{\"email\":\"v" + victim.id()
                + "@example.com\",\"fullName\":\"Ten\"}").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Upload: có giới hạn riêng chặt hơn request ghi thường (burst 2), file hợp lệ vẫn được nhận trong giới hạn")
    void uploadsHaveStricterLimit() throws Exception {
        String base = "/api/sections/" + sectionId + "/materials";
        assertThat(upload(base, false, lecturerA, "a.pdf", PDF, "title", "A").getStatus()).isEqualTo(201);
        assertThat(upload(base, false, lecturerA, "b.pdf", PDF, "title", "B").getStatus()).isEqualTo(201);
        MockHttpServletResponse limited = upload(base, false, lecturerA, "c.pdf", PDF, "title", "C");
        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getHeader("Retry-After")).isNotBlank();
        assertThat(json(limited).path("code").asString()).isEqualTo("TOO_MANY_REQUESTS");
        // request JSON thường vẫn còn trong hạn mức ghi
        assertThat(call(put("/api/users/me"), lecturerA, "{\"email\":\"l" + lecturerA.id()
                + "@example.com\",\"fullName\":\"Ten\"}").getStatus()).isEqualTo(200);
    }

    // ---------- đăng nhập ----------

    @Test
    @DisplayName("Login: sai mật khẩu quá ngưỡng -> 429 kể cả với mật khẩu đúng; không chặn người khác hay IP khác; đăng nhập đúng xóa bộ đếm")
    void loginBruteForceIsLimited(CapturedOutput output) throws Exception {
        String victim = usernameOf(account(RoleName.STUDENT));
        String bystander = usernameOf(account(RoleName.STUDENT));
        String secret = "MatKhauSaiBiMat!";

        for (int i = 0; i < 3; i++) {
            assertThat(login(victim, secret, "10.0.0.1").getStatus()).isEqualTo(401);
        }
        MockHttpServletResponse blocked = login(victim, PASSWORD, "10.0.0.1");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(Long.parseLong(blocked.getHeader("Retry-After"))).isPositive();
        assertThat(json(blocked).path("code").asString()).isEqualTo("TOO_MANY_REQUESTS");
        assertThat(blocked.getContentAsString()).doesNotContain("token").doesNotContain("accessToken");

        // không chặn nhầm: tài khoản khác cùng IP, và chính tài khoản đó từ IP khác
        assertThat(login(bystander, PASSWORD, "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(login(victim, PASSWORD, "10.0.0.2").getStatus()).isEqualTo(200);

        // đăng nhập đúng xóa bộ đếm của cặp: sai 2 lần, đúng 1 lần, rồi sai 2 lần nữa vẫn chưa bị chặn
        String careful = usernameOf(account(RoleName.STUDENT));
        for (int round = 0; round < 3; round++) {
            assertThat(login(careful, secret, "10.0.0.3").getStatus()).isEqualTo(401);
            assertThat(login(careful, secret, "10.0.0.3").getStatus()).isEqualTo(401);
            assertThat(login(careful, PASSWORD, "10.0.0.3").getStatus()).isEqualTo(200);
        }

        // không ghi mật khẩu hay token vào log
        assertThat(output.getAll()).doesNotContain(secret).doesNotContain(PASSWORD).doesNotContain("eyJ");
    }

    @Test
    @DisplayName("Login: dò nhiều tài khoản từ một IP bị chặn theo giới hạn IP; đăng nhập thành công không bị tính")
    void loginIpLimit() throws Exception {
        for (int i = 0; i < 8; i++) {
            assertThat(login("khong_ton_tai_" + i, "SaiMatKhau123", "10.1.1.1").getStatus()).isEqualTo(401);
        }
        MockHttpServletResponse blocked = login("khong_ton_tai_moi", "SaiMatKhau123", "10.1.1.1");
        assertThat(blocked.getStatus()).isEqualTo(429);

        // nhiều người đăng nhập đúng từ một IP (lớp học dùng chung mạng) không bao giờ bị chặn
        for (int i = 0; i < 12; i++) {
            Account student = account(RoleName.STUDENT);
            assertThat(login(usernameOf(student), PASSWORD, "10.2.2.2").getStatus()).isEqualTo(200);
        }
    }
}
