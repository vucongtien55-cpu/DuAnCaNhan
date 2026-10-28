package com.lms.service;

import com.lms.dto.auth.LoginRequest;
import com.lms.dto.auth.LoginResponse;
import com.lms.dto.user.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Đăng nhập, đăng xuất và thông tin người dùng hiện tại.
 * <ul>
 *   <li>Đăng nhập bằng tên đăng nhập hoặc email; sai thông tin luôn báo cùng một thông điệp chung (không lộ tài khoản
 *       có tồn tại hay không). Tài khoản bị khóa chỉ được báo sau khi mật khẩu đúng.</li>
 *   <li>Đăng xuất thu hồi ngay token đang dùng (lưu {@code jti} đến khi token hết hạn). Các token khác của cùng người
 *       dùng (thiết bị khác) vẫn còn hiệu lực đến khi hết hạn.</li>
 * </ul>
 */
public interface AuthService {

    LoginResponse login(@Valid @NotNull LoginRequest request);

    UserResponse currentUser(@NotNull Long userId);

    /** Thu hồi token có {@code jti} đến hết hạn {@code expiresAt}; gọi lại nhiều lần không gây lỗi. */
    void logout(@NotNull String jti, @NotNull Instant expiresAt);

    /** Xóa các bản ghi thu hồi của token đã hết hạn. @return số bản ghi đã xóa */
    int purgeExpiredRevokedTokens();
}
