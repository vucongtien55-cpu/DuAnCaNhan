package org.example.ridehailing.banhangonline.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.entity.AccountUnlockAudit;
import org.example.ridehailing.banhangonline.exception.DuplicateResourceException;
import org.example.ridehailing.banhangonline.repository.AccountUnlockAuditRepository;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.example.ridehailing.banhangonline.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final int NAME_MAX_LENGTH = 120;
    private static final int PHONE_MAX_LENGTH = 20;
    private static final int AVATAR_URL_MAX_LENGTH = 255;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final AccountUnlockAuditRepository accountUnlockAuditRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            AccountUnlockAuditRepository accountUnlockAuditRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountUnlockAuditRepository = accountUnlockAuditRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User registerCustomer(String fullName, String email, String rawPassword, String phone) {
        String normalizedEmail = normalizeEmail(email);
        validateFullName(fullName);
        validatePassword(rawPassword);
        validateOptionalLength(phone, PHONE_MAX_LENGTH, "Số điện thoại");
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email đã được đăng ký.");
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole("CUSTOMER");
        user.setEnabled(true);
        user.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        return userRepository.save(user);
    }

    @Override
    public User getUser(Long actorUserId, Long userId) {
        User actor = getEnabledUser(actorUserId);
        requirePositiveId(userId, "userId");
        if (!actor.getId().equals(userId) && !"ADMIN".equals(actor.getRole())) {
            throw new AccessDeniedException("Bạn không có quyền xem tài khoản này.");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản."));
    }

    @Override
    @Transactional
    public User updateProfile(Long actorUserId, String fullName, String phone, String avatarUrl) {
        User user = getEnabledUser(actorUserId);
        validateFullName(fullName);
        validateOptionalLength(phone, PHONE_MAX_LENGTH, "Số điện thoại");
        validateOptionalLength(avatarUrl, AVATAR_URL_MAX_LENGTH, "URL ảnh đại diện");

        user.setFullName(fullName.trim());
        user.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        user.setAvatarUrl(avatarUrl == null || avatarUrl.isBlank() ? null : avatarUrl.trim());
        return userRepository.save(user);
    }

    @Override
    public List<User> searchUsers(Long actorUserId, String role, Boolean enabled) {
        requireAdmin(actorUserId);
        String normalizedRole = role == null || role.isBlank() ? null : role.trim().toUpperCase(Locale.ROOT);
        if (normalizedRole != null && !List.of("ADMIN", "SHOP_OWNER", "EMPLOYEE", "CUSTOMER")
                .contains(normalizedRole)) {
            throw new IllegalArgumentException("Vai trò tài khoản không hợp lệ.");
        }
        if (normalizedRole != null && enabled != null) {
            return userRepository.findByRoleAndEnabled(normalizedRole, enabled);
        }
        if (normalizedRole != null) {
            return userRepository.findByRole(normalizedRole);
        }
        if (enabled != null) {
            return userRepository.findByEnabled(enabled);
        }
        return userRepository.findAll();
    }

    @Override
    public List<User> getLockedUsers(Long actorUserId) {
        requireAdmin(actorUserId);
        return userRepository.findByLockedTrue();
    }

    @Override
    @Transactional
    public User unlockAccount(Long actorUserId, Long accountId) {
        User administrator = requireAdmin(actorUserId);
        requirePositiveId(accountId, "accountId");
        User account = userRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Không tìm thấy tài khoản cần mở khóa."));
        if (!Boolean.TRUE.equals(account.getLocked())) {
            throw new IllegalArgumentException("Tài khoản hiện không bị khóa.");
        }

        account.setLocked(false);
        account.setFailedLoginAttempts(0);
        account.setLockedAt(null);
        account.setLastFailedLoginAt(null);
        accountUnlockAuditRepository.save(createUnlockAudit(account, administrator));
        return userRepository.save(account);
    }

    private AccountUnlockAudit createUnlockAudit(User account, User administrator) {
        AccountUnlockAudit audit = new AccountUnlockAudit();
        audit.setAccount(account);
        audit.setUnlockedBy(administrator);
        return audit;
    }

    private User requireAdmin(Long actorUserId) {
        User actor = getEnabledUser(actorUserId);
        if (!"ADMIN".equals(actor.getRole())) {
            throw new AccessDeniedException("Chỉ quản trị viên được thực hiện thao tác này.");
        }
        return actor;
    }

    private User getEnabledUser(Long userId) {
        requirePositiveId(userId, "actorUserId");
        return userRepository.findById(userId)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản đang hoạt động."));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email không được để trống.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 180 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Email không hợp lệ hoặc vượt quá 180 ký tự.");
        }
        return normalized;
    }

    private void validateFullName(String fullName) {
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Họ tên phải có từ 1 đến 120 ký tự.");
        }
    }

    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8
                || rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự và tối đa 72 byte UTF-8.");
        }
    }

    private void validateOptionalLength(String value, int maxLength, String label) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(label + " không được vượt quá " + maxLength + " ký tự.");
        }
    }

    private void requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " phải là số dương.");
        }
    }
}
