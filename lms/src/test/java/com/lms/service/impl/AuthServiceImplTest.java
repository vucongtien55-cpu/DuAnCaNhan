package com.lms.service.impl;

import com.lms.dto.auth.LoginRequest;
import com.lms.dto.auth.LoginResponse;
import com.lms.dto.user.UserResponse;
import com.lms.entity.RevokedToken;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.entity.enums.RoleName;
import com.lms.exception.ForbiddenException;
import com.lms.repository.RevokedTokenRepository;
import com.lms.repository.UserRepository;
import com.lms.security.JwtTokenService;
import com.lms.security.JwtTokenService.IssuedToken;
import com.lms.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private UserRepository userRepository;
    @Mock
    private RevokedTokenRepository revokedTokenRepository;
    @Mock
    private UserService userService;
    @Mock
    private JwtTokenService jwtTokenService;

    private PasswordEncoder passwordEncoder;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordEncoder = Mockito.spy(new BCryptPasswordEncoder(4));
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new AuthServiceImpl(userRepository, revokedTokenRepository, userService, passwordEncoder,
                jwtTokenService, clock);
        Mockito.clearInvocations(passwordEncoder);
    }

    private User user(boolean enabled) {
        Role role = new Role();
        role.setName(RoleName.STUDENT);
        User user = new User();
        user.setId(7L);
        user.setUsername("sv01");
        user.setEmail("sv01@example.com");
        user.setFullName("Sinh Vien");
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setEnabled(enabled);
        user.setRoles(Set.of(role));
        Mockito.clearInvocations(passwordEncoder);
        return user;
    }

    private IssuedToken stubToken(User user) {
        IssuedToken token = new IssuedToken("jwt-value", NOW.plusSeconds(3600), "jti-1");
        when(jwtTokenService.issue(user)).thenReturn(token);
        return token;
    }

    @Test
    @DisplayName("Đăng nhập đúng bằng tên đăng nhập: trả access token, thời hạn tính theo giây, không có mật khẩu")
    void login_byUsername() {
        User user = user(true);
        when(userRepository.findByUsername("sv01")).thenReturn(Optional.of(user));
        stubToken(user);

        LoginResponse response = service.login(new LoginRequest("  sv01 ", "Password123"));

        assertThat(response.accessToken()).isEqualTo("jwt-value");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(response.expiresAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(response.user().username()).isEqualTo("sv01");
        assertThat(response.user().roles()).containsExactly(RoleName.STUDENT);
        assertThat(response.toString()).doesNotContain("jwt-value").doesNotContain("Password123");
    }

    @Test
    @DisplayName("Đăng nhập bằng email (không phân biệt hoa thường)")
    void login_byEmail() {
        User user = user(true);
        when(userRepository.findByEmail("sv01@example.com")).thenReturn(Optional.of(user));
        stubToken(user);

        LoginResponse response = service.login(new LoginRequest("SV01@Example.com", "Password123"));

        assertThat(response.user().id()).isEqualTo(7L);
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("Sai mật khẩu -> BadCredentialsException, không phát hành token")
    void login_wrongPassword() {
        User existing = user(true);
        when(userRepository.findByUsername("sv01")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.login(new LoginRequest("sv01", "SaiMatKhau1")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(AuthServiceImpl.BAD_CREDENTIALS_MESSAGE);

        verify(jwtTokenService, never()).issue(any());
    }

    @Test
    @DisplayName("Tài khoản không tồn tại: cùng thông điệp với sai mật khẩu và vẫn tốn một lần so khớp mật khẩu")
    void login_unknownUser() {
        when(userRepository.findByUsername("khongco")).thenReturn(Optional.empty());
        User existing = user(true);
        when(userRepository.findByUsername("sv01")).thenReturn(Optional.of(existing));

        Throwable unknown = org.assertj.core.api.Assertions.catchThrowable(
                () -> service.login(new LoginRequest("khongco", "Password123")));
        Throwable wrongPassword = org.assertj.core.api.Assertions.catchThrowable(
                () -> service.login(new LoginRequest("sv01", "SaiMatKhau1")));

        assertThat(unknown).isInstanceOf(BadCredentialsException.class);
        assertThat(unknown.getMessage()).isEqualTo(wrongPassword.getMessage());
        // Cả hai trường hợp đều chạy BCrypt, nên không thể phân biệt bằng độ trễ.
        verify(passwordEncoder, times(2)).matches(any(), any());
        verify(jwtTokenService, never()).issue(any());
    }

    @Test
    @DisplayName("Email không tồn tại -> BadCredentialsException")
    void login_unknownEmail() {
        when(userRepository.findByEmail("khongco@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("khongco@example.com", "Password123")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Tài khoản bị khóa và mật khẩu đúng -> ForbiddenException, không phát hành token")
    void login_disabledAccount() {
        User disabled = user(false);
        when(userRepository.findByUsername("sv01")).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> service.login(new LoginRequest("sv01", "Password123")))
                .isInstanceOf(ForbiddenException.class);

        verify(jwtTokenService, never()).issue(any());
    }

    @Test
    @DisplayName("Tài khoản bị khóa nhưng mật khẩu sai: vẫn chỉ báo sai thông tin (không lộ trạng thái khóa)")
    void login_disabledAccountWrongPassword() {
        User disabled = user(false);
        when(userRepository.findByUsername("sv01")).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> service.login(new LoginRequest("sv01", "SaiMatKhau1")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Thông tin người dùng hiện tại lấy qua UserService")
    void currentUser() {
        UserResponse expected = UserResponse.from(user(true));
        when(userService.getById(7L)).thenReturn(expected);

        assertThat(service.currentUser(7L)).isSameAs(expected);
    }

    @Test
    @DisplayName("Đăng xuất lưu jti và thời điểm hết hạn của token để thu hồi")
    void logout_revokesToken() {
        when(revokedTokenRepository.existsByJti("jti-1")).thenReturn(false);

        service.logout("jti-1", NOW.plusSeconds(600));

        ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getJti()).isEqualTo("jti-1");
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(NOW.plusSeconds(600));
    }

    @Test
    @DisplayName("Đăng xuất lần nữa với cùng token: không lỗi, không lưu trùng")
    void logout_idempotent() {
        when(revokedTokenRepository.existsByJti("jti-1")).thenReturn(true);

        service.logout("jti-1", NOW.plusSeconds(600));

        verify(revokedTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dọn token hết hạn theo thời điểm hiện tại")
    void purgeExpired() {
        when(revokedTokenRepository.deleteExpired(NOW)).thenReturn(3);

        assertThat(service.purgeExpiredRevokedTokens()).isEqualTo(3);
    }

    @Test
    @DisplayName("LoginRequest không in mật khẩu khi ghi log")
    void loginRequestToStringMasksPassword() {
        assertThat(new LoginRequest("sv01", "Password123").toString())
                .contains("sv01").doesNotContain("Password123");
    }
}
