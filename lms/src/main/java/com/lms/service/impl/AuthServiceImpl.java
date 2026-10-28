package com.lms.service.impl;

import com.lms.dto.auth.LoginRequest;
import com.lms.dto.auth.LoginResponse;
import com.lms.dto.user.UserResponse;
import com.lms.entity.RevokedToken;
import com.lms.entity.User;
import com.lms.exception.ForbiddenException;
import com.lms.repository.RevokedTokenRepository;
import com.lms.repository.UserRepository;
import com.lms.security.JwtTokenService;
import com.lms.security.JwtTokenService.IssuedToken;
import com.lms.service.AuthService;
import com.lms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Validated
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    static final String BAD_CREDENTIALS_MESSAGE = "Tên đăng nhập hoặc mật khẩu không đúng";

    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final Clock clock;
    /** Băm giả để thời gian xử lý khi không có tài khoản xấp xỉ khi có tài khoản (chống dò tài khoản qua độ trễ). */
    private final String dummyHash;

    public AuthServiceImpl(UserRepository userRepository, RevokedTokenRepository revokedTokenRepository,
                           UserService userService, PasswordEncoder passwordEncoder,
                           JwtTokenService jwtTokenService, Clock clock) {
        this.userRepository = userRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String identifier = request.username().trim();
        // Tên đăng nhập không chứa '@' (đã ràng buộc khi tạo tài khoản) nên '@' nghĩa là đăng nhập bằng email.
        Optional<User> found = identifier.contains("@")
                ? userRepository.findByEmail(identifier.toLowerCase(Locale.ROOT))
                : userRepository.findByUsername(identifier);

        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            log.info("Đăng nhập thất bại");
            throw new BadCredentialsException(BAD_CREDENTIALS_MESSAGE);
        }
        User user = found.get();
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.info("Đăng nhập thất bại");
            throw new BadCredentialsException(BAD_CREDENTIALS_MESSAGE);
        }
        // Chỉ báo tài khoản bị khóa khi người gọi đã chứng minh biết đúng mật khẩu.
        if (!user.isEnabled()) {
            log.info("Đăng nhập bị từ chối: tài khoản {} đã bị khóa", user.getId());
            throw new ForbiddenException("Tài khoản đã bị khóa");
        }

        IssuedToken token = jwtTokenService.issue(user);
        log.info("Đăng nhập thành công: người dùng {}", user.getId());
        long expiresIn = Math.max(0, Duration.between(clock.instant(), token.expiresAt()).getSeconds());
        return new LoginResponse(token.value(), "Bearer", expiresIn, token.expiresAt(), UserResponse.from(user));
    }

    @Override
    public UserResponse currentUser(Long userId) {
        return userService.getById(userId);
    }

    @Override
    @Transactional
    public void logout(String jti, Instant expiresAt) {
        if (revokedTokenRepository.existsByJti(jti)) {
            return;
        }
        RevokedToken revoked = new RevokedToken();
        revoked.setJti(jti);
        revoked.setExpiresAt(expiresAt);
        revokedTokenRepository.save(revoked);
    }

    @Override
    @Transactional
    public int purgeExpiredRevokedTokens() {
        return revokedTokenRepository.deleteExpired(clock.instant());
    }
}
