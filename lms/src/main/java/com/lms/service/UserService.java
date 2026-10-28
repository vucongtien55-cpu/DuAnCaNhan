package com.lms.service;

import com.lms.dto.user.ChangePasswordRequest;
import com.lms.dto.user.CreateUserRequest;
import com.lms.dto.user.UpdateUserRequest;
import com.lms.dto.user.UserResponse;
import com.lms.entity.enums.RoleName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

/**
 * Nghiệp vụ tài khoản. Việc chỉ ADMIN được tạo/quản lý tài khoản sẽ được
 * áp dụng ở tầng bảo mật (Controller/Security), không kiểm tra trong service.
 */
public interface UserService {

    UserResponse createUser(@Valid CreateUserRequest request);

    UserResponse getById(Long id);

    UserResponse getByUsername(String username);

    Page<UserResponse> list(Pageable pageable);

    UserResponse updateProfile(Long id, @Valid UpdateUserRequest request);

    /** Người dùng tự đổi mật khẩu, phải đúng mật khẩu cũ. */
    void changePassword(Long id, @Valid ChangePasswordRequest request);

    /** Admin đặt lại mật khẩu, không cần mật khẩu cũ. */
    void resetPassword(Long id, @NotBlank @Size(min = 8, max = 100) String newPassword);

    /** Khóa hoặc mở tài khoản. Không cho khóa Admin cuối cùng. */
    UserResponse setEnabled(Long id, boolean enabled);

    /** Thay toàn bộ vai trò. Không cho gỡ quyền Admin cuối cùng. */
    UserResponse assignRoles(Long id, @NotEmpty Set<RoleName> roles);
}
