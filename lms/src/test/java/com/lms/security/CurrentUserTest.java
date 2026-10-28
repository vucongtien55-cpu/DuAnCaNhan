package com.lms.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserTest {

    private final CurrentUser currentUser = new CurrentUser();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Trả về mã người dùng của phiên đăng nhập")
    void returnsId() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("42", "x", "ROLE_STUDENT"));

        assertThat(currentUser.id()).isEqualTo(42L);
    }

    @Test
    @DisplayName("Chưa đăng nhập -> lỗi xác thực")
    void notAuthenticated() {
        assertThatThrownBy(currentUser::id).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @DisplayName("Người dùng ẩn danh -> lỗi xác thực")
    void anonymous() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThatThrownBy(currentUser::id).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @DisplayName("Tên không phải số -> lỗi xác thực")
    void nonNumericName() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("abc", "x", "ROLE_STUDENT"));

        assertThatThrownBy(currentUser::id).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
