package com.lms.dto.user;

import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.entity.enums.RoleName;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        String avatarUrl,
        boolean enabled,
        Set<RoleName> roles,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.isEnabled(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                user.getCreatedAt());
    }
}
