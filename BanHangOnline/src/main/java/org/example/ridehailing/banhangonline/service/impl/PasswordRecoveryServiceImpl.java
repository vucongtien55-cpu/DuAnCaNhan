package org.example.ridehailing.banhangonline.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.ridehailing.banhangonline.entity.PasswordResetToken;
import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.exception.PasswordResetUnavailableException;
import org.example.ridehailing.banhangonline.event.PasswordResetRequested;
import org.example.ridehailing.banhangonline.repository.PasswordResetTokenRepository;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.example.ridehailing.banhangonline.service.PasswordRecoveryService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class PasswordRecoveryServiceImpl implements PasswordRecoveryService {

    private static final int MAX_REQUESTS_PER_HOUR = 3;
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final String mailHost;
    private final String senderAddress;

    public PasswordRecoveryServiceImpl(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${app.mail.from:}") String senderAddress) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.mailHost = mailHost;
        this.senderAddress = senderAddress;
    }

    @Override
    @Transactional
    public void requestReset(String email) {
        if (mailHost.isBlank() || senderAddress.isBlank()) {
            throw new PasswordResetUnavailableException(
                    "Chức năng khôi phục mật khẩu chưa được cấu hình email gửi thư.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email không được để trống.");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailForUpdate(normalizedEmail).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        long recentRequests = tokenRepository.countByUser_IdAndCreatedAtAfter(
                user.getId(), now.minusHours(1));
        if (recentRequests >= MAX_REQUESTS_PER_HOUR) {
            return;
        }

        tokenRepository.expireUnusedByUserId(user.getId(), now);
        String rawToken = generateToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(sha256(rawToken));
        token.setExpiresAt(now.plusMinutes(30));
        tokenRepository.save(token);

        eventPublisher.publishEvent(new PasswordResetRequested(user.getEmail(), rawToken));
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        validatePassword(newPassword);
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 128) {
            throw new IllegalArgumentException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
        }
        PasswordResetToken candidate = tokenRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn."));
        User user = userRepository.findByIdForUpdate(candidate.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản."));
        PasswordResetToken token = tokenRepository.findByTokenHashForUpdate(sha256(rawToken))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn."));
        LocalDateTime now = LocalDateTime.now();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
        }

        if (!token.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        token.setUsedAt(now);
        tokenRepository.save(token);
        tokenRepository.expireUnusedByUserId(user.getId(), now);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        validatePassword(newPassword);
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không được để trống.");
        }
        User user = userRepository.findByIdForUpdate(userId)
                .filter(account -> Boolean.TRUE.equals(account.getEnabled()))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản đang hoạt động."));
        if (Boolean.TRUE.equals(user.getLocked())) {
            throw new AccessDeniedException("Tài khoản đang bị khóa.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AccessDeniedException("Mật khẩu hiện tại không chính xác.");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.expireUnusedByUserId(user.getId(), LocalDateTime.now());
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Không hỗ trợ thuật toán băm token an toàn.", exception);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất 8 ký tự và tối đa 72 byte UTF-8.");
        }
    }
}
