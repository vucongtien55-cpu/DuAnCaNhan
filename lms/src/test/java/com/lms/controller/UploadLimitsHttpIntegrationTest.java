package com.lms.controller;

import com.lms.dto.course.CreateCourseRequest;
import com.lms.dto.section.CreateSectionRequest;
import com.lms.dto.user.CreateUserRequest;
import com.lms.entity.User;
import com.lms.entity.enums.RoleName;
import com.lms.repository.CourseRepository;
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.MaterialRepository;
import com.lms.repository.UserRepository;
import com.lms.security.JwtTokenService;
import com.lms.service.CourseSectionService;
import com.lms.service.CourseService;
import com.lms.service.MaterialService;
import com.lms.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử giới hạn upload qua HTTP thật tới Tomcat nhúng (MockMvc không có servlet container nên không kiểm chứng được
 * giới hạn multipart). Dữ liệu được commit thật nên được dọn trong {@code @AfterEach}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"app.admin.username=", "app.admin.password=",
        "app.storage.upload-dir=build/test-uploads"})
class UploadLimitsHttpIntegrationTest {

    private static final int MB = 1024 * 1024;

    @LocalServerPort int port;
    @Autowired JsonMapper jsonMapper;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired CourseService courseService;
    @Autowired CourseRepository courseRepository;
    @Autowired CourseSectionService sectionService;
    @Autowired CourseSectionRepository sectionRepository;
    @Autowired MaterialService materialService;
    @Autowired MaterialRepository materialRepository;
    @Autowired JwtTokenService tokenService;

    private final HttpClient client = HttpClient.newHttpClient();
    private Long adminId;
    private Long studentId;
    private Long courseId;
    private Long sectionId;
    private String adminToken;
    private String studentToken;

    private Long newUser(RoleName role) {
        String name = "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return userService.createUser(new CreateUserRequest(name, name + "@example.com", "Password123",
                "Nguoi dung", null, Set.of(role))).id();
    }

    @BeforeEach
    void setUp() {
        adminId = newUser(RoleName.ADMIN);
        studentId = newUser(RoleName.STUDENT);
        User admin = userRepository.findById(adminId).orElseThrow();
        User student = userRepository.findById(studentId).orElseThrow();
        adminToken = tokenService.issue(admin).value();
        studentToken = tokenService.issue(student).value();
        String code = "UP" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        courseId = courseService.create(adminId, new CreateCourseRequest(code, "Mon thu upload", null, 3, null)).id();
        sectionId = sectionService.create(adminId, new CreateSectionRequest(courseId, null, "Lop upload", null, null,
                null, 10, null, null)).id();
    }

    @AfterEach
    void cleanUp() {
        materialRepository.findAll().stream()
                .filter(m -> m.getSection().getId().equals(sectionId))
                .forEach(m -> materialService.delete(adminId, m.getId()));
        sectionRepository.deleteById(sectionId);
        courseRepository.deleteById(courseId);
        userRepository.deleteById(studentId);
        userRepository.deleteById(adminId);
    }

    // ---------- helpers ----------

    private static byte[] pdf(int size) {
        byte[] data = new byte[size];
        byte[] header = "%PDF-1.4\n".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(header, 0, data, 0, header.length);
        return data;
    }

    private HttpResponse<String> uploadMaterial(String token, String fileName, byte[] content, Long targetSection)
            throws Exception {
        String boundary = "----lms" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream(content.length + 1024);
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"title\"\r\n\r\nTai lieu\r\n"
                + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName
                + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(content);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest.Builder request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/sections/" + targetSection + "/materials"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) throws Exception {
        return jsonMapper.readTree(response.body());
    }

    // ---------- tests ----------

    @Test
    @DisplayName("HTTP thật: file PDF hợp lệ (nhỏ và gần 20 MB) tải lên thành công 201")
    void validFilesAccepted() throws Exception {
        HttpResponse<String> small = uploadMaterial(adminToken, "nho.pdf", pdf(1024), sectionId);
        assertThat(small.statusCode()).isEqualTo(201);
        assertThat(json(small).path("originalFileName").asString()).isEqualTo("nho.pdf");

        HttpResponse<String> large = uploadMaterial(adminToken, "lon.pdf", pdf(20 * MB - 1024), sectionId);
        assertThat(large.statusCode()).isEqualTo(201);
    }

    @Test
    @DisplayName("HTTP thật: file vượt 20 MB bị từ chối nhất quán bằng 413 PAYLOAD_TOO_LARGE (JSON ApiError), không lưu file")
    void oversizedFileRejectedWith413() throws Exception {
        long before = materialRepository.count();
        for (int size : new int[]{20 * MB + 1, 22 * MB, 40 * MB}) {
            HttpResponse<String> response = uploadMaterial(adminToken, "qua-lon.pdf", pdf(size), sectionId);
            assertThat(response.statusCode()).as("size=" + size).isEqualTo(413);
            assertThat(json(response).path("code").asString()).isEqualTo("PAYLOAD_TOO_LARGE");
        }
        assertThat(materialRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("HTTP thật: sai định dạng (đuôi lạ, nội dung giả) -> 400; sinh viên -> 403; chưa đăng nhập -> 401")
    void wrongTypeAndAccess() throws Exception {
        assertThat(uploadMaterial(adminToken, "virus.exe", pdf(1024), sectionId).statusCode()).isEqualTo(400);
        assertThat(uploadMaterial(adminToken, "gia.pdf", "khong phai pdf".getBytes(StandardCharsets.UTF_8),
                sectionId).statusCode()).isEqualTo(400);
        assertThat(uploadMaterial(studentToken, "bai.pdf", pdf(1024), sectionId).statusCode()).isEqualTo(403);
        assertThat(uploadMaterial(null, "bai.pdf", pdf(1024), sectionId).statusCode()).isEqualTo(401);
        assertThat(uploadMaterial(adminToken, "bai.pdf", pdf(1024), 999999999L).statusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("HTTP thật: file quá lớn gửi bởi người chưa đăng nhập không được xử lý (401, hoặc kết nối bị ngắt), không lưu gì")
    void oversizedAnonymousUploadIsNotProcessed() throws Exception {
        long before = materialRepository.count();
        try {
            HttpResponse<String> response = uploadMaterial(null, "qua-lon.pdf", pdf(30 * MB), sectionId);
            assertThat(response.statusCode()).isEqualTo(401);
        } catch (java.io.IOException connectionClosed) {
            // Tomcat được phép đóng kết nối khi body vượt ngưỡng nuốt (max-swallow-size); chấp nhận được với request ẩn danh.
        }
        assertThat(materialRepository.count()).isEqualTo(before);
    }
}
