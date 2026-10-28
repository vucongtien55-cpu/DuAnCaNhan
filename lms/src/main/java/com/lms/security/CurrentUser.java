package com.lms.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Người đang đăng nhập. Mọi Controller phải lấy {@code actorId} từ đây, tuyệt đối không nhận từ dữ liệu client gửi lên.
 */
@Component
public class CurrentUser {

    public Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AuthenticationCredentialsNotFoundException("Chưa đăng nhập");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new AuthenticationCredentialsNotFoundException("Phiên đăng nhập không hợp lệ");
        }
    }
}
