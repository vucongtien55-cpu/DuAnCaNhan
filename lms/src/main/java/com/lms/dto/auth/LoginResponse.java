package com.lms.dto.auth;

import com.lms.dto.user.UserResponse;

import java.time.Instant;

/** Kết quả đăng nhập. Frontend gửi token trong header {@code Authorization: Bearer <accessToken>}. */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant expiresAt,
        UserResponse user) {

    /** Không để lộ token khi đối tượng bị in ra log. */
    @Override
    public String toString() {
        return "LoginResponse[accessToken=***, tokenType=" + tokenType + ", expiresAt=" + expiresAt
                + ", userId=" + (user == null ? null : user.id()) + "]";
    }
}
