package com.lms.dto.user;

import com.lms.entity.User;

/**
 * Thông tin tối thiểu để chọn một người dùng trong giao diện (không có email, số điện thoại, vai trò hay trạng thái).
 * Dùng cho các API tra cứu dành cho người không phải Admin.
 */
public record UserSummaryResponse(Long id, String username, String fullName) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getUsername(), user.getFullName());
    }
}
