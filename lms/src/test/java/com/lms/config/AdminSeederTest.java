package com.lms.config;

import com.lms.dto.user.CreateUserRequest;
import com.lms.entity.enums.RoleName;
import com.lms.exception.BusinessException;
import com.lms.repository.UserRepository;
import com.lms.service.UserService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AdminSeederTest {

    private static final String SECRET_PASSWORD = "S3cret-Admin-Pass!";

    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;

    private AdminSeeder seeder(String username, String password, String email) {
        return new AdminSeeder(new AdminSeedProperties(username, password, email, "Quản trị viên"),
                userService, userRepository);
    }

    @Test
    @DisplayName("Không cấu hình biến môi trường: không tạo tài khoản nào (không có Admin mặc định)")
    void nothingConfigured() {
        seeder("", "", "").run();
        seeder(null, null, null).run();

        verify(userService, never()).createUser(any());
        verify(userRepository, never()).countByRoles_NameAndEnabledTrue(any());
    }

    @Test
    @DisplayName("Chỉ có tên đăng nhập hoặc chỉ có mật khẩu: không tạo tài khoản")
    void incompleteConfiguration(CapturedOutput output) {
        seeder("admin", "", "").run();
        seeder("", SECRET_PASSWORD, "").run();

        verify(userService, never()).createUser(any());
        assertThat(output.getAll()).doesNotContain(SECRET_PASSWORD);
    }

    @Test
    @DisplayName("Cấu hình hợp lệ và chưa có Admin: tạo Admin với đúng vai trò, email mặc định theo tên đăng nhập")
    void createsFirstAdmin(CapturedOutput output) {
        when(userRepository.countByRoles_NameAndEnabledTrue(RoleName.ADMIN)).thenReturn(0L);

        seeder("  root.admin ", SECRET_PASSWORD, "").run();

        ArgumentCaptor<CreateUserRequest> captor = ArgumentCaptor.forClass(CreateUserRequest.class);
        verify(userService).createUser(captor.capture());
        CreateUserRequest request = captor.getValue();
        assertThat(request.username()).isEqualTo("root.admin");
        assertThat(request.email()).isEqualTo("root.admin@lms.local");
        assertThat(request.password()).isEqualTo(SECRET_PASSWORD);
        assertThat(request.roles()).containsExactly(RoleName.ADMIN);
        assertThat(output.getAll()).doesNotContain(SECRET_PASSWORD);
    }

    @Test
    @DisplayName("Dùng email cấu hình khi có")
    void usesConfiguredEmail() {
        when(userRepository.countByRoles_NameAndEnabledTrue(RoleName.ADMIN)).thenReturn(0L);

        seeder("admin", SECRET_PASSWORD, "quantri@truong.edu.vn").run();

        ArgumentCaptor<CreateUserRequest> captor = ArgumentCaptor.forClass(CreateUserRequest.class);
        verify(userService).createUser(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("quantri@truong.edu.vn");
    }

    @Test
    @DisplayName("Đã có Admin đang hoạt động: bỏ qua, không tạo thêm hay ghi đè")
    void adminAlreadyExists() {
        when(userRepository.countByRoles_NameAndEnabledTrue(RoleName.ADMIN)).thenReturn(1L);

        seeder("admin", SECRET_PASSWORD, "").run();

        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("Mật khẩu không hợp lệ: không tạo tài khoản, không làm hỏng khởi động và không ghi mật khẩu vào log")
    void invalidPasswordDoesNotLeakOrCrash(CapturedOutput output) {
        when(userRepository.countByRoles_NameAndEnabledTrue(RoleName.ADMIN)).thenReturn(0L);
        // Vi phạm thật do Bean Validation sinh ra cho mật khẩu quá ngắn
        Set<ConstraintViolation<CreateUserRequest>> violations = Validation.buildDefaultValidatorFactory()
                .getValidator().validate(new CreateUserRequest("admin", "admin@lms.local", "short", "Admin", null,
                        Set.of(RoleName.ADMIN)));
        assertThat(violations).isNotEmpty();
        ConstraintViolationException exception = new ConstraintViolationException(violations);
        when(userService.createUser(any())).thenThrow(exception);

        seeder("admin", "short", "").run();

        assertThat(output.getAll()).contains("không hợp lệ").doesNotContain("short\"");
    }

    @Test
    @DisplayName("Tên đăng nhập đã thuộc tài khoản khác: không ghi đè, chỉ cảnh báo")
    void usernameTaken(CapturedOutput output) {
        when(userRepository.countByRoles_NameAndEnabledTrue(RoleName.ADMIN)).thenReturn(0L);
        when(userService.createUser(any())).thenThrow(new BusinessException("Tên đăng nhập đã tồn tại: admin"));

        seeder("admin", SECRET_PASSWORD, "").run();

        assertThat(output.getAll()).contains("Không thể tạo tài khoản Admin").doesNotContain(SECRET_PASSWORD);
    }

    @Test
    @DisplayName("Cấu hình không in mật khẩu khi bị ghi log")
    void propertiesToStringMasksPassword() {
        assertThat(new AdminSeedProperties("admin", SECRET_PASSWORD, "a@b.c", "Admin").toString())
                .contains("admin").doesNotContain(SECRET_PASSWORD);
    }
}
