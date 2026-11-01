package com.lms.controller;

import com.lms.dto.user.ChangePasswordRequest;
import com.lms.dto.user.CreateUserRequest;
import com.lms.dto.user.UpdateUserRequest;
import com.lms.dto.user.UserResponse;
import com.lms.entity.enums.RoleName;
import com.lms.security.CurrentUser;
import com.lms.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 100) String newPassword) {
    }

    public record SetEnabledRequest(@NotNull Boolean enabled) {
    }

    public record AssignRolesRequest(@NotEmpty Set<RoleName> roles) {
    }

    /** Người dùng xem hồ sơ của chính mình; danh tính lấy từ JWT đã xác thực. */
    @GetMapping("/me")
    public UserResponse getMyProfile() {
        return userService.getById(currentUser.id());
    }

    /** Người dùng tự cập nhật hồ sơ. */
    @PutMapping("/me")
    public UserResponse updateMyProfile(@Valid @RequestBody UpdateUserRequest request) {
        return userService.updateProfile(currentUser.id(), request);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUser.id(), request);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Page<UserResponse> list(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) RoleName role,
                                   @RequestParam(required = false) Boolean enabled,
                                   @ParameterObject Pageable pageable) {
        return userService.search(keyword, role, enabled, PageRequests.restrict(pageable, Sort.by("id"),
                "id", "username", "email", "fullName", "enabled", "createdAt"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateProfile(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/enabled")
    public UserResponse setEnabled(@PathVariable Long id, @Valid @RequestBody SetEnabledRequest request) {
        return userService.setEnabled(id, request.enabled());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/roles")
    public UserResponse assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesRequest request) {
        return userService.assignRoles(id, request.roles());
    }
}
