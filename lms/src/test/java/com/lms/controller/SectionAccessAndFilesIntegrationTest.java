package com.lms.controller;

import com.lms.dto.user.CreateUserRequest;
import com.lms.entity.User;
import com.lms.entity.enums.RoleName;
import com.lms.repository.UserRepository;
import com.lms.security.JwtTokenService;
import com.lms.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Kiểm thử HTTP cho Section, Enrollment, Assignment và Material: quyền theo vai trò và theo từng lớp học phần (giảng viên
 * lớp khác, sinh viên chưa đăng ký), cùng upload/download file (loại file, chữ ký nội dung, dung lượng, header tải về).
 * Dữ liệu nằm trong giao dịch được hoàn tác.
 */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
@TestPropertySource(properties = {"app.admin.username=", "app.admin.password=",
        "app.storage.upload-dir=build/test-uploads"})
class SectionAccessAndFilesIntegrationTest {

    private static final String PASSWORD = "Password123";
    private static final byte[] PDF = "%PDF-1.4\nnoi dung thu nghiem".getBytes(StandardCharsets.UTF_8);

    @Autowired MockMvc mockMvc;
    @Autowired JsonMapper jsonMapper;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired JwtTokenService tokenService;

    private record Account(Long id, String token) {
    }

    private Account admin;
    private Account lecturerA;
    private Account lecturerB;
    private Account studentIn;
    private Account studentOut;
    private long sectionId;
    private String sectionCode;

    // ---------- helpers ----------

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private Account account(RoleName role) {
        String name = "t" + suffix();
        Long id = userService.createUser(new CreateUserRequest(name, name + "@example.com", PASSWORD,
                "Nguoi dung " + name, null, Set.of(role))).id();
        User user = userRepository.findById(id).orElseThrow();
        return new Account(id, tokenService.issue(user).value());
    }

    private MockHttpServletResponse call(AbstractMockHttpServletRequestBuilder<?> request, Account who, String body)
            throws Exception {
        if (who != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + who.token());
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request).andReturn().getResponse();
    }

    private MockHttpServletResponse upload(String url, boolean replace, Account who, String fileName, byte[] content,
                                           String... fields) throws Exception {
        var builder = multipart(url).file(new MockMultipartFile("file", fileName, "application/pdf", content));
        for (int i = 0; i < fields.length; i += 2) {
            builder.param(fields[i], fields[i + 1]);
        }
        if (replace) {
            builder.with(request -> {
                request.setMethod("PUT");
                return request;
            });
        }
        return call(builder, who, null);
    }

    private JsonNode json(MockHttpServletResponse response) throws Exception {
        return jsonMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() throws Exception {
        admin = account(RoleName.ADMIN);
        lecturerA = account(RoleName.LECTURER);
        lecturerB = account(RoleName.LECTURER);
        studentIn = account(RoleName.STUDENT);
        studentOut = account(RoleName.STUDENT);

        String courseCode = "C" + suffix().toUpperCase();
        MockHttpServletResponse course = call(post("/api/courses"), admin,
                "{\"code\":\"" + courseCode + "\",\"title\":\"Mon hoc\",\"credits\":3}");
        long courseId = json(course).path("id").asLong();
        call(put("/api/courses/" + courseId), admin,
                "{\"code\":\"" + courseCode + "\",\"title\":\"Mon hoc\",\"credits\":3,\"status\":\"ACTIVE\"}");

        MockHttpServletResponse section = call(post("/api/sections"), admin,
                "{\"courseId\":" + courseId + ",\"name\":\"Lop 1\",\"maxStudents\":30,\"teacherId\":"
                        + lecturerA.id() + "}");
        assertThat(section.getStatus()).isEqualTo(201);
        sectionId = json(section).path("id").asLong();
        sectionCode = json(section).path("sectionCode").asString();
        assertThat(call(put("/api/sections/" + sectionId + "/status"), admin,
                "{\"status\":\"OPEN_REGISTRATION\"}").getStatus()).isEqualTo(200);

        MockHttpServletResponse enrolled = call(post("/api/enrollments"), studentIn,
                "{\"sectionCode\":\"" + sectionCode + "\"}");
        assertThat(enrolled.getStatus()).isEqualTo(201);
    }

    // ---------- Section ----------

    @Test
    @DisplayName("Section: chưa đăng nhập 401; chỉ Admin tạo/gán giảng viên/đổi trạng thái/sinh lại mã (403 với vai trò khác)")
    void sectionManagementIsAdminOnly() throws Exception {
        assertThat(call(get("/api/sections"), null, null).getStatus()).isEqualTo(401);
        assertThat(call(get("/api/sections/" + sectionId), null, null).getStatus()).isEqualTo(401);

        String create = "{\"courseId\":1,\"name\":\"X\",\"maxStudents\":10}";
        for (Account who : new Account[]{lecturerA, studentIn}) {
            assertThat(call(post("/api/sections"), who, create).getStatus()).isEqualTo(403);
            assertThat(call(put("/api/sections/" + sectionId + "/teacher"), who,
                    "{\"teacherId\":" + who.id() + "}").getStatus()).isEqualTo(403);
            assertThat(call(post("/api/sections/" + sectionId + "/regenerate-code"), who, null).getStatus())
                    .isEqualTo(403);
        }
        for (Account who : new Account[]{studentIn, lecturerB}) {
            assertThat(call(put("/api/sections/" + sectionId + "/status"), who,
                    "{\"status\":\"ENDED\"}").getStatus()).isEqualTo(403);
        }
        assertThat(call(put("/api/sections/" + sectionId + "/teacher"), admin,
                "{\"teacherId\":" + studentIn.id() + "}").getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Section: sinh viên chưa đăng ký không thấy lớp chưa mở (404/403); sinh viên đã đăng ký và giảng viên phụ trách thấy lớp")
    void sectionVisibility() throws Exception {
        assertThat(call(get("/api/sections/" + sectionId), studentIn, null).getStatus()).isEqualTo(200);
        assertThat(call(get("/api/sections/" + sectionId), lecturerA, null).getStatus()).isEqualTo(200);
        assertThat(call(get("/api/sections/by-code/" + sectionCode), studentOut, null).getStatus()).isEqualTo(200);

        call(put("/api/sections/" + sectionId + "/status"), admin, "{\"status\":\"IN_PROGRESS\"}");
        assertThat(call(get("/api/sections/" + sectionId), studentOut, null).getStatus()).isIn(403, 404);
        assertThat(call(get("/api/sections/" + sectionId), studentIn, null).getStatus()).isEqualTo(200);
        assertThat(call(get("/api/sections/999999999"), admin, null).getStatus()).isEqualTo(404);
    }

    // ---------- Enrollment ----------

    @Test
    @DisplayName("Enrollment: danh tính sinh viên lấy từ JWT, studentId trong body bị bỏ qua; đăng ký trùng 400; giảng viên không tự đăng ký")
    void enrollmentUsesJwtIdentity() throws Exception {
        String body = "{\"sectionCode\":\"" + sectionCode + "\",\"studentId\":" + studentIn.id() + "}";
        assertThat(call(post("/api/enrollments"), null, body).getStatus()).isEqualTo(401);
        assertThat(call(post("/api/enrollments"), studentIn, body).getStatus()).isEqualTo(400);
        assertThat(call(post("/api/enrollments"), lecturerA, body).getStatus()).isEqualTo(403);

        MockHttpServletResponse out = call(post("/api/enrollments"), studentOut,
                "{\"sectionCode\":\"" + sectionCode + "\",\"studentId\":" + studentIn.id() + "}");
        assertThat(out.getStatus()).isEqualTo(201);
        assertThat(json(out).path("studentId").asLong()).isEqualTo(studentOut.id());

        MockHttpServletResponse mine = call(get("/api/enrollments/me"), studentOut, null);
        assertThat(mine.getStatus()).isEqualTo(200);
        assertThat(json(mine).size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Enrollment: chỉ giảng viên phụ trách/Admin xem danh sách, thêm, gỡ sinh viên; giảng viên lớp khác và sinh viên bị 403")
    void enrollmentManagementPerSection() throws Exception {
        String list = "/api/sections/" + sectionId + "/enrollments";
        assertThat(call(get(list), null, null).getStatus()).isEqualTo(401);
        assertThat(call(get(list), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(get(list), lecturerB, null).getStatus()).isEqualTo(403);
        assertThat(call(get(list), lecturerA, null).getStatus()).isEqualTo(200);
        assertThat(call(get(list), admin, null).getStatus()).isEqualTo(200);

        String add = "{\"studentId\":" + studentOut.id() + "}";
        assertThat(call(post(list), studentIn, add).getStatus()).isEqualTo(403);
        assertThat(call(post(list), lecturerB, add).getStatus()).isEqualTo(403);
        assertThat(call(post(list), lecturerA, add).getStatus()).isEqualTo(201);

        String withdraw = list + "/" + studentOut.id() + "/withdraw";
        assertThat(call(post(withdraw), lecturerB, null).getStatus()).isEqualTo(403);
        MockHttpServletResponse dropped = call(post(withdraw), lecturerA, null);
        assertThat(dropped.getStatus()).isEqualTo(200);
        assertThat(json(dropped).path("status").asString()).isEqualTo("DROPPED");
    }

    // ---------- Assignment ----------

    private String assignmentBody() {
        return "{\"sectionId\":" + sectionId + ",\"title\":\"Bai tap 1\",\"maxScore\":10}";
    }

    @Test
    @DisplayName("Assignment: chỉ giảng viên phụ trách/Admin tạo, sửa, xóa; sinh viên đã đăng ký chỉ xem; người ngoài lớp bị 403")
    void assignmentAccessPerSection() throws Exception {
        assertThat(call(post("/api/assignments"), null, assignmentBody()).getStatus()).isEqualTo(401);
        assertThat(call(post("/api/assignments"), studentIn, assignmentBody()).getStatus()).isEqualTo(403);
        assertThat(call(post("/api/assignments"), lecturerB, assignmentBody()).getStatus()).isEqualTo(403);

        MockHttpServletResponse created = call(post("/api/assignments"), lecturerA, assignmentBody());
        assertThat(created.getStatus()).isEqualTo(201);
        long id = json(created).path("id").asLong();

        String list = "/api/sections/" + sectionId + "/assignments";
        assertThat(call(get(list), studentIn, null).getStatus()).isEqualTo(200);
        assertThat(call(get(list), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(get(list), lecturerB, null).getStatus()).isEqualTo(403);
        assertThat(call(get("/api/assignments/" + id), studentIn, null).getStatus()).isEqualTo(200);
        assertThat(call(get("/api/assignments/" + id), studentOut, null).getStatus()).isEqualTo(403);

        String update = "{\"title\":\"Sua\",\"maxScore\":10,\"allowLateSubmission\":true,\"gradingPolicy\":\"LATEST\"}";
        assertThat(call(put("/api/assignments/" + id), studentIn, update).getStatus()).isEqualTo(403);
        assertThat(call(put("/api/assignments/" + id), lecturerB, update).getStatus()).isEqualTo(403);
        assertThat(call(delete("/api/assignments/" + id), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(delete("/api/assignments/" + id), lecturerB, null).getStatus()).isEqualTo(403);
        assertThat(call(delete("/api/assignments/" + id), lecturerA, null).getStatus()).isEqualTo(204);

        assertThat(call(post("/api/assignments"), lecturerA, "{\"sectionId\":" + sectionId + "}").getStatus())
                .isEqualTo(400);
    }

    @Test
    @DisplayName("Assignment attachment: giảng viên phụ trách tải lên PDF; sinh viên trong lớp tải về; người ngoài lớp và vai trò sai bị 403")
    void assignmentAttachmentFlow() throws Exception {
        long id = json(call(post("/api/assignments"), lecturerA, assignmentBody())).path("id").asLong();
        String url = "/api/assignments/" + id + "/attachment";

        assertThat(upload(url, true, null, "de.pdf", PDF).getStatus()).isEqualTo(401);
        assertThat(upload(url, true, studentIn, "de.pdf", PDF).getStatus()).isEqualTo(403);
        assertThat(upload(url, true, lecturerB, "de.pdf", PDF).getStatus()).isEqualTo(403);
        assertThat(upload(url, true, lecturerA, "de.pdf", PDF).getStatus()).isEqualTo(200);

        MockHttpServletResponse download = call(get(url), studentIn, null);
        assertThat(download.getStatus()).isEqualTo(200);
        assertThat(download.getContentAsByteArray()).isEqualTo(PDF);
        assertThat(download.getHeader(HttpHeaders.CONTENT_DISPOSITION)).startsWith("attachment");
        assertThat(download.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");

        assertThat(call(get(url), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(get(url), null, null).getStatus()).isEqualTo(401);
        assertThat(call(delete(url), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(delete(url), lecturerA, null).getStatus()).isEqualTo(200);
    }

    // ---------- Material ----------

    @Test
    @DisplayName("Material: giảng viên phụ trách tải lên, sinh viên trong lớp tải về với header an toàn; người ngoài lớp bị 403")
    void materialAccessAndDownload() throws Exception {
        String base = "/api/sections/" + sectionId + "/materials";
        assertThat(upload(base, false, null, "bai1.pdf", PDF, "title", "Bai 1").getStatus()).isEqualTo(401);
        assertThat(upload(base, false, studentIn, "bai1.pdf", PDF, "title", "Bai 1").getStatus()).isEqualTo(403);
        assertThat(upload(base, false, lecturerB, "bai1.pdf", PDF, "title", "Bai 1").getStatus()).isEqualTo(403);

        MockHttpServletResponse created = upload(base, false, lecturerA, "bai1.pdf", PDF, "title", "Bai 1");
        assertThat(created.getStatus()).isEqualTo(201);
        long id = json(created).path("id").asLong();
        assertThat(created.getContentAsString(StandardCharsets.UTF_8)).doesNotContain("uploads");

        assertThat(call(get(base), studentIn, null).getStatus()).isEqualTo(200);
        assertThat(call(get(base), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(get(base), lecturerB, null).getStatus()).isEqualTo(403);

        MockHttpServletResponse download = call(get("/api/materials/" + id + "/download"), studentIn, null);
        assertThat(download.getStatus()).isEqualTo(200);
        assertThat(download.getContentAsByteArray()).isEqualTo(PDF);
        assertThat(download.getContentType()).isEqualTo("application/pdf");
        assertThat(download.getHeader(HttpHeaders.CONTENT_DISPOSITION)).startsWith("attachment");
        assertThat(download.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");

        assertThat(call(get("/api/materials/" + id + "/download"), studentOut, null).getStatus()).isEqualTo(403);
        assertThat(call(get("/api/materials/" + id + "/download"), null, null).getStatus()).isEqualTo(401);
        assertThat(call(get("/api/materials/999999999/download"), admin, null).getStatus()).isEqualTo(404);
        assertThat(call(delete("/api/materials/" + id), studentIn, null).getStatus()).isEqualTo(403);
        assertThat(call(delete("/api/materials/" + id), lecturerB, null).getStatus()).isEqualTo(403);
        assertThat(call(delete("/api/materials/" + id), lecturerA, null).getStatus()).isEqualTo(204);
    }

    @Test
    @DisplayName("Upload: từ chối đuôi file không cho phép, nội dung không khớp định dạng, file rỗng, vượt 20 MB, thiếu tiêu đề")
    void uploadRestrictions() throws Exception {
        String base = "/api/sections/" + sectionId + "/materials";
        byte[] exe = "MZ\u0090\u0000".getBytes(StandardCharsets.ISO_8859_1);
        assertThat(upload(base, false, lecturerA, "virus.exe", exe, "title", "T").getStatus()).isEqualTo(400);
        assertThat(upload(base, false, lecturerA, "page.html", "<script>".getBytes(), "title", "T").getStatus())
                .isEqualTo(400);
        assertThat(upload(base, false, lecturerA, "gia.pdf", "khong phai pdf".getBytes(), "title", "T").getStatus())
                .isEqualTo(400);
        assertThat(upload(base, false, lecturerA, "gia.docx", PDF, "title", "T").getStatus()).isEqualTo(400);
        assertThat(upload(base, false, lecturerA, "rong.pdf", new byte[0], "title", "T").getStatus())
                .isEqualTo(400);
        assertThat(upload(base, false, lecturerA, "khongduoi", PDF, "title", "T").getStatus()).isEqualTo(400);
        assertThat(upload(base, false, lecturerA, "bai.pdf", PDF).getStatus()).isEqualTo(400);

        byte[] tooBig = new byte[20 * 1024 * 1024 + 1];
        System.arraycopy(PDF, 0, tooBig, 0, PDF.length);
        assertThat(upload(base, false, lecturerA, "lon.pdf", tooBig, "title", "T").getStatus()).isEqualTo(400);

        byte[] zip = {'P', 'K', 3, 4, 0, 0};
        assertThat(upload(base, false, lecturerA, "ok.zip", zip, "title", "Zip").getStatus()).isEqualTo(201);
    }

    @Test
    @DisplayName("Upload: tên file chứa đường dẫn (../) được làm sạch, không ghi ra ngoài thư mục lưu trữ")
    void uploadPathTraversalNameIsSanitized() throws Exception {
        String base = "/api/sections/" + sectionId + "/materials";
        MockHttpServletResponse created =
                upload(base, false, lecturerA, "../../etc/passwd.pdf", PDF, "title", "Traversal");
        assertThat(created.getStatus()).isEqualTo(201);
        assertThat(json(created).toString()).doesNotContain("..");
    }
}
