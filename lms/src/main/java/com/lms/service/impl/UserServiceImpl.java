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
import com.lms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String username = request.username().trim();
        String email = normalizeEmail(request.email());

        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("Tên đăng nhập đã tồn tại: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email đã được sử dụng: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone());
        user.setRoles(resolveRoles(request.roles()));
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public UserResponse getById(Long id) {
        return UserResponse.from(findUser(id));
    }

    @Override
    public UserResponse getByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", username));
    }

    @Override
    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long id, UpdateUserRequest request) {
        User user = findUser(id);
        String email = normalizeEmail(request.email());

        if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new BusinessException("Email đã được sử dụng: " + email);
        }

        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone());
        user.setAvatarUrl(request.avatarUrl());
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = findUser(id);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu cũ không đúng");
        }
        if (request.oldPassword().equals(request.newPassword())) {
            throw new BusinessException("Mật khẩu mới phải khác mật khẩu cũ");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        findUser(id).setPassword(passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled) {
        User user = findUser(id);
        if (!enabled && user.isEnabled()) {
            ensureNotLastAdmin(user);
        }
        user.setEnabled(enabled);
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public UserResponse assignRoles(Long id, Set<RoleName> roles) {
        User user = findUser(id);
        Set<Role> newRoles = resolveRoles(roles);
        if (!roles.contains(RoleName.ADMIN)) {
            ensureNotLastAdmin(user);
        }
        user.setRoles(newRoles);
        return UserResponse.from(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", id));
    }

    private Set<Role> resolveRoles(Set<RoleName> names) {
        Set<Role> roles = new HashSet<>();
        for (RoleName name : names) {
            roles.add(roleRepository.findByName(name)
                    .orElseThrow(() -> new ResourceNotFoundException("Vai trò", name)));
        }
        return roles;
    }

    /** Chặn thao tác làm hệ thống không còn Admin nào hoạt động. */
    private void ensureNotLastAdmin(User user) {
        boolean isActiveAdmin = user.isEnabled()
                && user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN);
        if (isActiveAdmin && userRepository.countByRoles_NameAndEnabledTrue(RoleName.ADMIN) <= 1) {
            throw new BusinessException("Không thể khóa hoặc gỡ quyền Admin cuối cùng của hệ thống");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
