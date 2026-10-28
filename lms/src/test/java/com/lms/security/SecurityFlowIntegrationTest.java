package com.lms.security;

import com.lms.dto.user.CreateUserRequest;
import com.lms.entity.User;
import com.lms.entity.enums.RoleName;
import com.lms.repository.UserRepository;
import com.lms.service.UserService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Kiểm thử bảo mật qua HTTP thật (bộ lọc bảo mật, JWT, phân quyền, định dạng lỗi) trên PostgreSQL. Dữ liệu nằm trong một
 * giao dịch được hoàn tác khi kết thúc test nên không để lại gì trong database.
 */
@SpringBootTest
// MockMvc mặc định in nguyên request/response (kể cả mật khẩu, token) ra console; tắt để test log chỉ đo log của ứng dụng.
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = {"app.admin.username=", "app.admin.password="})
class SecurityFlowIntegrationTest {

    private static final String PASSWORD = "Password123";

    @Autowired MockMvc mockMvc;
    @Autowired JsonMapper jsonMapper;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired JwtTokenService tokenService;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JwtProperties jwtProperties;
    @PersistenceContext EntityManager em;

    // ---------- helpers ----------

    private Long createUser(String username, RoleName role) {
        return userService.createUser(new CreateUserRequest(username, username + "@example.com", PASSWORD,
                "Nguoi dung " + username, null, Set.of(role))).id();
    }

    private String loginBody(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private MvcResult login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(username, password))).andReturn();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String loginToken(String username) throws Exception {
        MvcResult result = login(username, PASSWORD);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return json(result).path("accessToken").asString();
    }

    private MockHttpServletResponse getWith(String path, String token) throws Exception {
        MockHttpServletRequestBuilder request = get(path);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(request).andReturn().getResponse();
    }

    private int statusOf(String path, String token) throws Exception {
        return getWith(path, token).getStatus();
    }

    private JsonNode body(MockHttpServletResponse response) throws Exception {
        return jsonMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    }

    /** Dựng token tùy ý bằng khóa của ứng dụng (hoặc khóa khác) để mô phỏng token lỗi. */
    private String craftToken(JwtEncoder encoder, String subject, String issuer, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(subject).id(UUID.randomUUID().toString())
                .issuedAt(issuedAt).expiresAt(expiresAt).claim("roles", java.util.List.of("ADMIN")).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    // ---------- đăng nhập ----------

    @Test
    @DisplayName("Đăng nhập đúng: trả token, không có mật khẩu/băm, không tạo session hay cookie, không ghi log bí mật")
    void loginSuccess(CapturedOutput output) throws Exception {
        createUser("it_sv1", RoleName.STUDENT);

        MvcResult result = login("it_sv1", PASSWORD);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = json(result);
        String token = body.path("accessToken").asString();
        assertThat(token).isNotBlank();
        assertThat(body.path("tokenType").asString()).isEqualTo("Bearer");
        assertThat(body.path("expiresIn").asLong()).isBetween(3500L, 3600L);
        assertThat(body.path("user").path("username").asString()).isEqualTo("it_sv1");
        assertThat(body.path("user").path("roles").get(0).asString()).isEqualTo("STUDENT");
        String raw = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(raw).doesNotContain(PASSWORD).doesNotContain("$2a$").doesNotContain("password");
        assertThat(result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-store");

        // Stateless: không có session, không có cookie xác thực (lý do CSRF không áp dụng)
        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();

        assertThat(output.getAll()).doesNotContain(PASSWORD).doesNotContain(token)
                .doesNotContain("Using generated security password");
    }

    @Test
    @DisplayName("Đăng nhập bằng email, không phân biệt hoa thường")
    void loginByEmail() throws Exception {
        createUser("it_sv2", RoleName.STUDENT);

        MvcResult result = login("IT_SV2@Example.com", PASSWORD);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(result).path("user").path("username").asString()).isEqualTo("it_sv2");
    }

    @Test
    @DisplayName("Sai mật khẩu và tài khoản không tồn tại cho cùng một phản hồi 401 (không lộ tài khoản)")
    void loginFailureIsUniform() throws Exception {
        createUser("it_sv3", RoleName.STUDENT);

        MvcResult wrongPassword = login("it_sv3", "SaiMatKhau99");
        MvcResult unknownUser = login("it_khong_co", PASSWORD);

        assertThat(wrongPassword.getResponse().getStatus()).isEqualTo(401);
        assertThat(unknownUser.getResponse().getStatus()).isEqualTo(401);
        JsonNode first = json(wrongPassword);
        JsonNode second = json(unknownUser);
        assertThat(first.path("code").asString()).isEqualTo("BAD_CREDENTIALS");
        assertThat(first.path("message").asString()).isEqualTo(second.path("message").asString());
        assertThat(first.path("status").asInt()).isEqualTo(401);
        assertThat(first.path("path").asString()).isEqualTo("/api/auth/login");
        assertThat(first.has("timestamp")).isTrue();
        assertThat(first.toString()).doesNotContain("it_sv3").doesNotContain("SaiMatKhau99");
    }

    @Test
    @DisplayName("Tài khoản bị khóa: đúng mật khẩu -> 403; sai mật khẩu -> 401 (không lộ trạng thái khóa)")
    void loginDisabledAccount() throws Exception {
        createUser("it_admin_a", RoleName.ADMIN);
        Long id = createUser("it_sv4", RoleName.STUDENT);
        userService.setEnabled(id, false);
        em.flush();

        assertThat(login("it_sv4", PASSWORD).getResponse().getStatus()).isEqualTo(403);
        assertThat(login("it_sv4", "SaiMatKhau99").getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Đăng nhập thiếu dữ liệu -> 400 VALIDATION_ERROR kèm chi tiết theo trường")
    void loginValidation() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"\",\"password\":\"\"}")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode body = json(result);
        assertThat(body.path("code").asString()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.path("details").size()).isEqualTo(2);
        assertThat(body.path("details").toString()).contains("username").contains("password");
    }

    @Test
    @DisplayName("Token cũ còn sót trong header không làm hỏng việc đăng nhập lại")
    void loginIgnoresStaleBearerHeader() throws Exception {
        createUser("it_sv5", RoleName.STUDENT);

        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-cu-da-het-han")
                .content(loginBody("it_sv5", PASSWORD))).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    // ---------- chưa đăng nhập ----------

    @Test
    @DisplayName("Chưa đăng nhập không vào được API được bảo vệ: 401 định dạng chuẩn, kể cả với POST (không bị CSRF 403)")
    void unauthenticatedRejected() throws Exception {
        MockHttpServletResponse get = getWith("/api/probe/authenticated", null);
        assertThat(get.getStatus()).isEqualTo(401);
        assertThat(get.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(body(get).path("code").asString()).isEqualTo("UNAUTHORIZED");

        MockHttpServletResponse post = mockMvc.perform(post("/api/probe/echo").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"a\",\"age\":1}")).andReturn().getResponse();
        assertThat(post.getStatus()).isEqualTo(401);

        assertThat(getWith("/api/auth/me", null).getStatus()).isEqualTo(401);
        assertThat(mockMvc.perform(post("/api/auth/logout")).andReturn().getResponse().getStatus()).isEqualTo(401);
        // Mọi đường dẫn ngoài /api đều bị chặn
        assertThat(getWith("/actuator/env", null).getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Token chỉ được nhận qua header Authorization, không nhận qua query string")
    void tokenInQueryStringRejected() throws Exception {
        createUser("it_sv6", RoleName.STUDENT);
        String token = loginToken("it_sv6");

        assertThat(statusOf("/api/probe/authenticated?access_token=" + token, null)).isEqualTo(401);
        assertThat(statusOf("/api/probe/authenticated", token)).isEqualTo(200);
    }

    // ---------- token không hợp lệ ----------

    @Test
    @DisplayName("Token sai: rác, bị sửa, ký bằng khóa khác, không ký, hết hạn, issuer lạ, subject không phải số -> 401")
    void invalidTokensRejected() throws Exception {
        Long id = createUser("it_sv7", RoleName.STUDENT);
        em.flush();
        String valid = loginToken("it_sv7");
        Instant now = Instant.now();

        String[] parts = valid.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                        .replace("\"" + id + "\"", "\"1\"").getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        SecretKey otherKey = new SecretKeySpec("ffffffffffffffffffffffffffffffff".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(otherKey));
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String unsigned = b64.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8)) + "."
                + b64.encodeToString(("{\"sub\":\"" + id + "\",\"iss\":\"lms\",\"jti\":\"x\",\"exp\":"
                + now.plusSeconds(600).getEpochSecond() + "}").getBytes(StandardCharsets.UTF_8)) + ".";

        String[] bad = {
                "khong-phai-jwt",
                tampered,
                craftToken(otherEncoder, String.valueOf(id), jwtProperties.issuer(), now, now.plusSeconds(600)),
                unsigned,
                craftToken(jwtEncoder, String.valueOf(id), jwtProperties.issuer(), now.minusSeconds(7200),
                        now.minusSeconds(3600)),
                craftToken(jwtEncoder, String.valueOf(id), "ben-khac", now, now.plusSeconds(600)),
                craftToken(jwtEncoder, "khong-phai-so", jwtProperties.issuer(), now, now.plusSeconds(600)),
                craftToken(jwtEncoder, "999999999", jwtProperties.issuer(), now, now.plusSeconds(600)),
        };
        for (int i = 0; i < bad.length; i++) {
            MockHttpServletResponse response = getWith("/api/probe/authenticated", bad[i]);
            assertThat(response.getStatus()).as("token xấu #" + i).isEqualTo(401);
            assertThat(body(response).path("code").asString()).as("token xấu #" + i).isEqualTo("UNAUTHORIZED");
            // Thông điệp chung chung, không tiết lộ token sai ở đâu
            assertThat(body(response).path("message").asString()).doesNotContainIgnoringCase("signature")
                    .doesNotContainIgnoringCase("expired");
        }
        assertThat(statusOf("/api/probe/authenticated", valid)).isEqualTo(200);
    }

    @Test
    @DisplayName("Token của tài khoản đã bị khóa hoặc đã bị xóa mất hiệu lực ngay, không chờ hết hạn")
    void tokenOfDisabledOrDeletedUserRejected() throws Exception {
        createUser("it_admin_b", RoleName.ADMIN);
        Long disabledId = createUser("it_sv8", RoleName.STUDENT);
        Long deletedId = createUser("it_sv9", RoleName.STUDENT);
        em.flush();
        String disabledToken = loginToken("it_sv8");
        String deletedToken = loginToken("it_sv9");
        assertThat(statusOf("/api/probe/authenticated", disabledToken)).isEqualTo(200);

        userService.setEnabled(disabledId, false);
        userRepository.deleteById(deletedId);
        em.flush();
        em.clear();

        assertThat(statusOf("/api/probe/authenticated", disabledToken)).isEqualTo(401);
        assertThat(statusOf("/api/probe/authenticated", deletedToken)).isEqualTo(401);
    }

    // ---------- phân quyền ----------

    @Test
    @DisplayName("Ma trận phân quyền: Admin, Giảng viên, Sinh viên chỉ vào đúng khu vực của mình (403 nếu sai vai trò)")
    void roleMatrix() throws Exception {
        createUser("it_admin_c", RoleName.ADMIN);
        createUser("it_lecturer", RoleName.LECTURER);
        createUser("it_student", RoleName.STUDENT);
        em.flush();
        String admin = loginToken("it_admin_c");
        String lecturer = loginToken("it_lecturer");
        String student = loginToken("it_student");

        // /admin: chỉ ADMIN
        assertThat(statusOf("/api/probe/admin", admin)).isEqualTo(200);
        assertThat(statusOf("/api/probe/admin", lecturer)).isEqualTo(403);
        assertThat(statusOf("/api/probe/admin", student)).isEqualTo(403);
        // /staff: ADMIN và LECTURER
        assertThat(statusOf("/api/probe/staff", admin)).isEqualTo(200);
        assertThat(statusOf("/api/probe/staff", lecturer)).isEqualTo(200);
        assertThat(statusOf("/api/probe/staff", student)).isEqualTo(403);
        // /student: chỉ STUDENT
        assertThat(statusOf("/api/probe/student", student)).isEqualTo(200);
        assertThat(statusOf("/api/probe/student", admin)).isEqualTo(403);
        assertThat(statusOf("/api/probe/student", lecturer)).isEqualTo(403);
        // Mọi người đã đăng nhập vào được khu vực chung
        assertThat(statusOf("/api/probe/authenticated", student)).isEqualTo(200);

        MockHttpServletResponse denied = getWith("/api/probe/admin", student);
        JsonNode deniedBody = body(denied);
        assertThat(deniedBody.path("code").asString()).isEqualTo("FORBIDDEN");
        assertThat(deniedBody.path("status").asInt()).isEqualTo(403);
        assertThat(body(getWith("/api/probe/authenticated", lecturer)).path("authorities").get(0).asString())
                .isEqualTo("ROLE_LECTURER");
    }

    @Test
    @DisplayName("Vai trò lấy từ database ở mỗi request: Admin bị hạ quyền mất quyền ngay dù token vẫn ghi ADMIN")
    void roleChangeAppliesImmediately() throws Exception {
        createUser("it_admin_d", RoleName.ADMIN);
        Long demotedId = createUser("it_admin_e", RoleName.ADMIN);
        em.flush();
        String token = loginToken("it_admin_e");
        assertThat(statusOf("/api/probe/admin", token)).isEqualTo(200);

        userService.assignRoles(demotedId, Set.of(RoleName.STUDENT));
        em.flush();
        em.clear();

        assertThat(statusOf("/api/probe/admin", token)).isEqualTo(403);
        assertThat(statusOf("/api/probe/student", token)).isEqualTo(200);
    }

    // ---------- thông tin người dùng hiện tại và đăng xuất ----------

    @Test
    @DisplayName("/api/auth/me trả đúng người dùng đang đăng nhập, không có mật khẩu")
    void me() throws Exception {
        createUser("it_sv10", RoleName.STUDENT);
        String token = loginToken("it_sv10");

        MockHttpServletResponse response = getWith("/api/auth/me", token);

        assertThat(response.getStatus()).isEqualTo(200);
        JsonNode body = body(response);
        assertThat(body.path("username").asString()).isEqualTo("it_sv10");
        assertThat(body.path("email").asString()).isEqualTo("it_sv10@example.com");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).doesNotContain("password").doesNotContain("$2a$");
    }

    @Test
    @DisplayName("Đăng xuất thu hồi ngay token đang dùng; token của phiên khác vẫn dùng được đến khi hết hạn")
    void logoutRevokesOnlyCurrentToken() throws Exception {
        createUser("it_sv11", RoleName.STUDENT);
        String first = loginToken("it_sv11");
        String second = loginToken("it_sv11");
        assertThat(first).isNotEqualTo(second);
        assertThat(statusOf("/api/auth/me", first)).isEqualTo(200);

        MockHttpServletResponse logout = mockMvc.perform(post("/api/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + first)).andReturn().getResponse();
        em.flush();
        em.clear();

        assertThat(logout.getStatus()).isEqualTo(204);
        assertThat(statusOf("/api/auth/me", first)).isEqualTo(401);
        assertThat(statusOf("/api/probe/authenticated", first)).isEqualTo(401);
        // Phiên khác (token khác) của cùng tài khoản không bị ảnh hưởng
        assertThat(statusOf("/api/auth/me", second)).isEqualTo(200);
        // Dùng lại token đã đăng xuất để đăng xuất tiếp cũng bị từ chối
        assertThat(mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + first))
                .andReturn().getResponse().getStatus()).isEqualTo(401);
    }

    // ---------- định dạng lỗi thống nhất ----------

    @Test
    @DisplayName("Các loại lỗi nghiệp vụ đều trả cùng một định dạng ApiError với mã trạng thái đúng")
    void unifiedErrorFormat() throws Exception {
        createUser("it_sv12", RoleName.STUDENT);
        String token = loginToken("it_sv12");

        assertError(getWith("/api/probe/business-error", token), 400, "BUSINESS_RULE_VIOLATION");
        assertError(getWith("/api/probe/not-found", token), 404, "NOT_FOUND");
        assertError(getWith("/api/probe/forbidden", token), 403, "FORBIDDEN");
        assertError(getWith("/api/probe/does-not-exist", token), 404, "NOT_FOUND");
    }

    private void assertError(MockHttpServletResponse response, int status, String code) throws Exception {
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = body(response);
        assertThat(body.path("status").asInt()).isEqualTo(status);
        assertThat(body.path("code").asString()).isEqualTo(code);
        assertThat(body.path("message").asString()).isNotBlank();
        assertThat(body.path("path").asString()).startsWith("/api/");
        assertThat(body.has("timestamp")).isTrue();
        assertThat(body.has("details")).isTrue();
    }

    @Test
    @DisplayName("Lỗi hệ thống không lộ chi tiết nội bộ (mật khẩu, địa chỉ database, đường dẫn máy chủ)")
    void internalErrorsAreSanitized() throws Exception {
        createUser("it_sv13", RoleName.STUDENT);
        String token = loginToken("it_sv13");

        MockHttpServletResponse boom = getWith("/api/probe/boom", token);
        MockHttpServletResponse storage = getWith("/api/probe/storage-error", token);

        assertError(boom, 500, "INTERNAL_ERROR");
        assertError(storage, 500, "STORAGE_ERROR");
        assertThat(boom.getContentAsString(StandardCharsets.UTF_8)).doesNotContain("hunter2")
                .doesNotContain("db-noi-bo").doesNotContain("IllegalStateException");
        assertThat(storage.getContentAsString(StandardCharsets.UTF_8)).doesNotContain("/var/lms");
    }

    @Test
    @DisplayName("Dữ liệu gửi lên sai: thiếu trường, JSON hỏng, sai phương thức -> lỗi 400/405 đúng định dạng")
    void requestErrors() throws Exception {
        createUser("it_sv14", RoleName.STUDENT);
        String token = loginToken("it_sv14");

        MockHttpServletResponse invalid = mockMvc.perform(post("/api/probe/echo").contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .content("{\"name\":\"\",\"age\":0}")).andReturn().getResponse();
        assertError(invalid, 400, "VALIDATION_ERROR");
        assertThat(body(invalid).path("details").toString()).contains("name").contains("age");

        MockHttpServletResponse malformed = mockMvc.perform(post("/api/probe/echo").contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .content("{khong phai json")).andReturn().getResponse();
        assertError(malformed, 400, "BAD_REQUEST");

        MockHttpServletResponse wrongMethod = mockMvc.perform(post("/api/probe/authenticated")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn().getResponse();
        assertError(wrongMethod, 405, "METHOD_NOT_ALLOWED");
    }

    // ---------- CORS ----------

    @Test
    @DisplayName("CORS: cho phép origin của frontend đã khai báo, từ chối origin lạ; không bật allow-credentials")
    void cors() throws Exception {
        MockHttpServletResponse allowed = mockMvc.perform(options("/api/auth/login")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andReturn().getResponse();
        assertThat(allowed.getStatus()).isEqualTo(200);
        assertThat(allowed.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://localhost:3000");
        assertThat(allowed.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isNull();
        assertThat(allowed.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).contains("POST");

        MockHttpServletResponse denied = mockMvc.perform(options("/api/auth/login")
                .header(HttpHeaders.ORIGIN, "https://trang-doc-hai.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")).andReturn().getResponse();
        assertThat(denied.getStatus()).isEqualTo(403);
        assertThat(denied.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();

        // Yêu cầu thật từ origin lạ cũng không nhận được header cho phép
        MockHttpServletResponse foreign = mockMvc.perform(get("/api/probe/authenticated")
                .header(HttpHeaders.ORIGIN, "https://trang-doc-hai.example")).andReturn().getResponse();
        assertThat(foreign.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    @Test
    @DisplayName("Header bảo mật mặc định có mặt và phản hồi API không bị cache")
    void securityHeaders() throws Exception {
        createUser("it_sv15", RoleName.STUDENT);
        String token = loginToken("it_sv15");

        MockHttpServletResponse response = getWith("/api/auth/me", token);

        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-store");
    }

    @Test
    @DisplayName("Dùng đối tượng User trực tiếp: token phát hành cho người dùng thật chứa claim tối thiểu")
    void issuedTokenCarriesMinimalClaims() throws Exception {
        Long id = createUser("it_sv16", RoleName.LECTURER);
        em.flush();
        User user = userRepository.findById(id).orElseThrow();

        String token = tokenService.issue(user).value();

        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        assertThat(payload).contains("\"sub\":\"" + id + "\"").contains("\"jti\"").contains("\"exp\"")
                .doesNotContain("password").doesNotContain("email").doesNotContain("$2a$");
    }
}
