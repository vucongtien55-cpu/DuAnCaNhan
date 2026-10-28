package com.lms.config;

import com.lms.dto.user.CreateUserRequest;
import com.lms.entity.enums.RoleName;
import com.lms.exception.BusinessException;
import com.lms.repository.UserRepository;
import com.lms.service.UserService;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tạo Admin đầu tiên khi khởi động, chỉ khi cả LMS_ADMIN_USERNAME và LMS_ADMIN_PASSWORD hợp lệ và hệ thống chưa có
 * Admin nào đang hoạt động. Không bao giờ tạo tài khoản mặc định, không ghi đè tài khoản có sẵn và không ghi mật khẩu
 * vào log.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final AdminSeedProperties properties;
    private final UserService userService;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (!properties.hasUsername() && !properties.hasPassword()) {
            log.info("Chưa cấu hình LMS_ADMIN_USERNAME/LMS_ADMIN_PASSWORD: không tạo tài khoản Admin");
            return;
        }
        if (!properties.hasUsername() || !properties.hasPassword()) {
            log.warn("Cần cấu hình đồng thời LMS_ADMIN_USERNAME và LMS_ADMIN_PASSWORD: không tạo tài khoản Admin");
            return;
        }
        if (userRepository.countByRoles_NameAndEnabledTrue(RoleName.ADMIN) > 0) {
            log.info("Hệ thống đã có Admin đang hoạt động: bỏ qua việc tạo Admin từ cấu hình");
            return;
        }

        String username = properties.username().trim();
        String email = properties.email() == null || properties.email().isBlank()
                ? username + "@lms.local" : properties.email().trim();
        try {
            userService.createUser(new CreateUserRequest(username, email, properties.password(),
                    properties.fullName(), null, Set.of(RoleName.ADMIN)));
            log.info("Đã tạo tài khoản Admin đầu tiên '{}'", username);
        } catch (ConstraintViolationException e) {
            // Chỉ ghi tên trường và thông điệp, không ghi giá trị đã nhập (có thể chứa mật khẩu).
            log.warn("Cấu hình Admin không hợp lệ, không tạo tài khoản: {}", e.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; ")));
        } catch (BusinessException e) {
            log.warn("Không thể tạo tài khoản Admin từ cấu hình: {}", e.getMessage());
        }
    }
}
