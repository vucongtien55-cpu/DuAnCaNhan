package com.lms.config;

import com.lms.entity.Role;
import com.lms.entity.enums.RoleName;
import com.lms.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Khởi tạo các Role mặc định khi ứng dụng chạy; bỏ qua Role đã tồn tại. */
@Component
@Order(1)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Map<RoleName, String> DEFAULT_ROLES = Map.of(
            RoleName.ADMIN, "Quản trị viên hệ thống",
            RoleName.LECTURER, "Giảng viên",
            RoleName.STUDENT, "Sinh viên");

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        DEFAULT_ROLES.forEach((name, description) -> {
            if (!roleRepository.existsByName(name)) {
                Role role = new Role();
                role.setName(name);
                role.setDescription(description);
                roleRepository.save(role);
            }
        });
    }
}
