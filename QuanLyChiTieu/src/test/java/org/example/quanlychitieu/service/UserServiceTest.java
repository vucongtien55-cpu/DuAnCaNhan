package org.example.quanlychitieu.service;

import org.example.quanlychitieu.entity.User;
import org.example.quanlychitieu.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho UserService.
 * Dùng Mockito để giả lập UserRepository => không cần database.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser() {
        return new User(1L, "tien@example.com", "matkhau123", "Vu Cong Tien", "0900000000");
    }

    @Test
    @DisplayName("Đăng nhập đúng email và mật khẩu -> trả về user")
    void authenticate_withCorrectPassword_returnsUser() {
        when(userRepository.findByEmail("tien@example.com")).thenReturn(Optional.of(sampleUser()));

        Optional<User> result = userService.authenticate("tien@example.com", "matkhau123");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Vu Cong Tien");
    }

    @Test
    @DisplayName("Đăng nhập sai mật khẩu -> trả về rỗng")
    void authenticate_withWrongPassword_returnsEmpty() {
        when(userRepository.findByEmail("tien@example.com")).thenReturn(Optional.of(sampleUser()));

        Optional<User> result = userService.authenticate("tien@example.com", "sai_mat_khau");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Đăng nhập với email không tồn tại -> trả về rỗng")
    void authenticate_withUnknownEmail_returnsEmpty() {
        when(userRepository.findByEmail("khongco@example.com")).thenReturn(Optional.empty());

        assertThat(userService.authenticate("khongco@example.com", "bat_ky")).isEmpty();
    }

    @Test
    @DisplayName("Đăng ký email mới -> lưu vào repository")
    void register_withNewEmail_savesUser() {
        User user = sampleUser();
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        User saved = userService.register(user);

        assertThat(saved.getEmail()).isEqualTo("tien@example.com");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Đăng ký email đã tồn tại -> ném IllegalArgumentException, không lưu")
    void register_withExistingEmail_throwsException() {
        User user = sampleUser();
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
        verify(userRepository, never()).save(any());
    }
}
