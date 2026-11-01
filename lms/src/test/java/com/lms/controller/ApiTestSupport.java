package com.lms.controller;

import com.lms.dto.user.CreateUserRequest;
import com.lms.entity.User;
import com.lms.entity.enums.RoleName;
import com.lms.repository.UserRepository;
import com.lms.security.JwtTokenService;
import com.lms.service.UserService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Nền cho test tích hợp HTTP: dựng sẵn Admin, hai giảng viên (A phụ trách lớp, B không), hai sinh viên (đã / chưa ghi
 * danh) và một lớp học phần đang mở đăng ký. Dữ liệu nằm trong giao dịch được hoàn tác sau mỗi test.
 */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
@TestPropertySource(properties = {"app.admin.username=", "app.admin.password=",
        "app.storage.upload-dir=build/test-uploads"})
abstract class ApiTestSupport {

    static final String PASSWORD = "Password123";
    static final byte[] PDF = "%PDF-1.4\nnoi dung thu nghiem".getBytes(StandardCharsets.UTF_8);

    @Autowired MockMvc mockMvc;
    @Autowired JsonMapper jsonMapper;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired JwtTokenService tokenService;

    record Account(Long id, String token) {
    }

    Account admin;
    Account lecturerA;
    Account lecturerB;
    Account studentIn;
    Account studentOut;
    long sectionId;
    String sectionCode;

    String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    Account account(RoleName role) {
        String name = "t" + suffix();
        Long id = userService.createUser(new CreateUserRequest(name, name + "@example.com", PASSWORD,
                "Nguoi dung " + name, null, Set.of(role))).id();
        User user = userRepository.findById(id).orElseThrow();
        return new Account(id, tokenService.issue(user).value());
    }

    MockHttpServletResponse call(AbstractMockHttpServletRequestBuilder<?> request, Account who, String body)
            throws Exception {
        if (who != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + who.token());
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request).andReturn().getResponse();
    }

    MockHttpServletResponse upload(String url, boolean replace, Account who, String fileName, byte[] content,
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

    JsonNode json(MockHttpServletResponse response) throws Exception {
        return jsonMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    }

    /** Tạo bài tập trong lớp bằng giảng viên phụ trách; {@code extraJson} là các field bổ sung bắt đầu bằng dấu phẩy. */
    long createAssignment(String extraJson) throws Exception {
        MockHttpServletResponse created = call(post("/api/assignments"), lecturerA,
                "{\"sectionId\":" + sectionId + ",\"title\":\"Bai tap\",\"maxScore\":10" + extraJson + "}");
        assertThat(created.getStatus()).isEqualTo(201);
        return json(created).path("id").asLong();
    }

    @BeforeEach
    void setUpBaseData() throws Exception {
        admin = account(RoleName.ADMIN);
        lecturerA = account(RoleName.LECTURER);
        lecturerB = account(RoleName.LECTURER);
        studentIn = account(RoleName.STUDENT);
        studentOut = account(RoleName.STUDENT);

        String courseCode = "C" + suffix().toUpperCase();
        String courseBody = "{\"code\":\"" + courseCode + "\",\"title\":\"Mon hoc\",\"credits\":3";
        long courseId = json(call(post("/api/courses"), admin, courseBody + "}")).path("id").asLong();
        call(put("/api/courses/" + courseId), admin, courseBody + ",\"status\":\"ACTIVE\"}");

        MockHttpServletResponse section = call(post("/api/sections"), admin,
                "{\"courseId\":" + courseId + ",\"name\":\"Lop 1\",\"maxStudents\":30,\"teacherId\":"
                        + lecturerA.id() + "}");
        assertThat(section.getStatus()).isEqualTo(201);
        sectionId = json(section).path("id").asLong();
        sectionCode = json(section).path("sectionCode").asString();
        assertThat(call(put("/api/sections/" + sectionId + "/status"), admin,
                "{\"status\":\"OPEN_REGISTRATION\"}").getStatus()).isEqualTo(200);
        assertThat(call(post("/api/enrollments"), studentIn, "{\"sectionCode\":\"" + sectionCode + "\"}")
                .getStatus()).isEqualTo(201);
    }
}
