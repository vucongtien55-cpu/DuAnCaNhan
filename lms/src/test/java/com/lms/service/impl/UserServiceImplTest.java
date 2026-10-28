package com.lms.service.impl;

import com.lms.dto.user.ChangePasswordRequest;
import com.lms.dto.user.CreateUserRequest;
import com.lms.dto.user.UpdateUserRequest;
import com.lms.dto.user.UserResponse;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.entity.enums.RoleName;
import com.lms.exception.BusinessException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.RoleRepository;
import com.lms.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String HASH = "$2a$10$hashedPasswordValue";

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    // ---------- helpers ----------

    private static Role role(RoleName name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    private static User user(Long id, String email, boolean enabled, RoleName... roleNames) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setEmail(email);
        user.setPassword(HASH);
        user.setFullName("Nguyen Van " + id);
        user.setEnabled(enabled);
        Set<Role> roles = new HashSet<>();
        Arrays.stream(roleNames).forEach(n -> roles.add(role(n)));
        user.setRoles(roles);
        return user;
    }

    private static CreateUserRequest createRequest(String email) {
        return new CreateUserRequest("student01", email, "Password123", "Nguyen Van A",
                "0901234567", Set.of(RoleName.STUDENT));
    }

    // ---------- createUser ----------

    @Test
    @DisplayName("Tạo tài khoản thành công: mã hóa mật khẩu, chuẩn hóa email, gán role")
    void createUser_success() {
        when(userRepository.existsByUsername("student01")).thenReturn(false);
        when(userRepository.existsByEmail("a@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(role(RoleName.STUDENT)));
        when(passwordEncoder.encode("Password123")).thenReturn(HASH);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserResponse response = userService.createUser(createRequest("  A@Example.com "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo(HASH).isNotEqualTo("Password123");
        assertThat(saved.getEmail()).isEqualTo("a@example.com");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("student01");
        assertThat(response.email()).isEqualTo("a@example.com");
        assertThat(response.roles()).containsExactly(RoleName.STUDENT);
        assertThat(response.enabled()).isTrue();
    }

    @Test
    @DisplayName("Tạo tài khoản với email đã tồn tại -> BusinessException")
    void createUser_duplicateEmail() {
        when(userRepository.existsByUsername("student01")).thenReturn(false);
        when(userRepository.existsByEmail("a@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(createRequest("a@example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo tài khoản với username đã tồn tại -> BusinessException")
    void createUser_duplicateUsername() {
        when(userRepository.existsByUsername("student01")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(createRequest("a@example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tên đăng nhập");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo tài khoản khi không tìm thấy Role -> ResourceNotFoundException")
    void createUser_roleNotFound() {
        when(userRepository.existsByUsername("student01")).thenReturn(false);
        when(userRepository.existsByEmail("a@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn(HASH);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(createRequest("a@example.com")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    // ---------- updateProfile ----------

    @Test
    @DisplayName("Cập nhật hồ sơ thành công")
    void updateProfile_success() {
        User existing = user(1L, "old@example.com", true, RoleName.STUDENT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        UserResponse response = userService.updateProfile(1L,
                new UpdateUserRequest("New@Example.com", "  Ten Moi ", "0909999999", "http://img/a.png"));

        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.fullName()).isEqualTo("Ten Moi");
        assertThat(response.phone()).isEqualTo("0909999999");
        assertThat(response.avatarUrl()).isEqualTo("http://img/a.png");
        assertThat(existing.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("Cập nhật hồ sơ với email đã thuộc người khác -> BusinessException")
    void updateProfile_emailTakenByOther() {
        User existing = user(1L, "old@example.com", true, RoleName.STUDENT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(1L,
                new UpdateUserRequest("taken@example.com", "Ten", null, null)))
                .isInstanceOf(BusinessException.class);

        assertThat(existing.getEmail()).isEqualTo("old@example.com");
    }

    @Test
    @DisplayName("Cập nhật hồ sơ giữ nguyên email -> không kiểm tra trùng")
    void updateProfile_sameEmail() {
        User existing = user(1L, "same@example.com", true, RoleName.STUDENT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        userService.updateProfile(1L, new UpdateUserRequest("same@example.com", "Ten", null, null));

        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    @DisplayName("Cập nhật hồ sơ của người không tồn tại -> ResourceNotFoundException")
    void updateProfile_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(99L,
                new UpdateUserRequest("a@example.com", "Ten", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- changePassword ----------

    @Test
    @DisplayName("Đổi mật khẩu thành công")
    void changePassword_success() {
        User existing = user(1L, "a@example.com", true, RoleName.STUDENT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("OldPass123", HASH)).thenReturn(true);
        when(passwordEncoder.encode("NewPass123")).thenReturn("newHash");

        userService.changePassword(1L, new ChangePasswordRequest("OldPass123", "NewPass123"));

        assertThat(existing.getPassword()).isEqualTo("newHash");
    }

    @Test
    @DisplayName("Đổi mật khẩu với mật khẩu cũ sai -> BusinessException")
    void changePassword_wrongOldPassword() {
        User existing = user(1L, "a@example.com", true, RoleName.STUDENT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("WrongPass1", HASH)).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L,
                new ChangePasswordRequest("WrongPass1", "NewPass123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mật khẩu cũ");

        assertThat(existing.getPassword()).isEqualTo(HASH);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Đổi mật khẩu với mật khẩu mới trùng mật khẩu cũ -> BusinessException")
    void changePassword_sameAsOld() {
        User existing = user(1L, "a@example.com", true, RoleName.STUDENT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("SamePass123", HASH)).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(1L,
                new ChangePasswordRequest("SamePass123", "SamePass123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("khác");

        assertThat(existing.getPassword()).isEqualTo(HASH);
        verify(passwordEncoder, never()).encode(any());
    }

    // ---------- setEnabled ----------

    @Test
    @DisplayName("Khóa sinh viên thông thường thành công, không cần kiểm tra Admin cuối")
    void setEnabled_disableStudent() {
        User student = user(1L, "s@example.com", true, RoleName.STUDENT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        UserResponse response = userService.setEnabled(1L, false);

        assertThat(response.enabled()).isFalse();
        verify(userRepository, never()).countByRoles_NameAndEnabledTrue(any());
    }

    @Test
    @DisplayName("Mở khóa tài khoản đang bị khóa thành công")
    void setEnabled_enable() {
        User student = user(1L, "s@example.com", false, RoleName.STUDENT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        UserResponse response = userService.setEnabled(1L, true);

        assertThat(response.enabled()).isTrue();
        verify(userRepository, never()).countByRoles_NameAndEnabledTrue(any());
    }

    @Test
    @DisplayName("Khóa Admin cuối cùng -> BusinessException")
    void setEnabled_disableLastAdmin() {
        User admin = user(1L, "admin@example.com", true, RoleName.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoles_NameAndEnabledTrue(RoleName.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.setEnabled(1L, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Admin cuối cùng");

        assertThat(admin.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Khóa Admin khi còn Admin khác đang hoạt động thành công")
    void setEnabled_disableAdminWhenOthersExist() {
        User admin = user(1L, "admin@example.com", true, RoleName.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoles_NameAndEnabledTrue(RoleName.ADMIN)).thenReturn(2L);

        UserResponse response = userService.setEnabled(1L, false);

        assertThat(response.enabled()).isFalse();
    }

    // ---------- assignRoles ----------

    @Test
    @DisplayName("Gỡ quyền Admin cuối cùng -> BusinessException")
    void assignRoles_removeLastAdmin() {
        User admin = user(1L, "admin@example.com", true, RoleName.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(roleRepository.findByName(RoleName.LECTURER)).thenReturn(Optional.of(role(RoleName.LECTURER)));
        when(userRepository.countByRoles_NameAndEnabledTrue(RoleName.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.assignRoles(1L, Set.of(RoleName.LECTURER)))
                .isInstanceOf(BusinessException.class);

        assertThat(admin.getRoles()).extracting(Role::getName).containsExactly(RoleName.ADMIN);
    }

    // ---------- UserResponse ----------

    @Test
    @DisplayName("UserResponse không chứa password")
    void userResponse_doesNotExposePassword() {
        User existing = user(1L, "a@example.com", true, RoleName.STUDENT);

        UserResponse response = UserResponse.from(existing);

        assertThat(Arrays.stream(UserResponse.class.getRecordComponents()).map(RecordComponent::getName))
                .noneMatch(name -> name.toLowerCase().contains("password"));
        assertThat(response.toString()).doesNotContain(HASH);
    }
}
