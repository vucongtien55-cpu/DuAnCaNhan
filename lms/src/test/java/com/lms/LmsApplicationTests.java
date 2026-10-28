package com.lms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// Không bao giờ để test tự tạo Admin trong database thật, kể cả khi máy có đặt LMS_ADMIN_*.
@SpringBootTest
@TestPropertySource(properties = {"app.admin.username=", "app.admin.password="})
class LmsApplicationTests {

    @Test
    void contextLoads() {
    }

}
