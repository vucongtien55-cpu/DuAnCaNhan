package com.lms.security;

import com.lms.entity.User;
import com.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Chuyển JWT đã được xác thực chữ ký thành thông tin đăng nhập, nhưng <b>lấy vai trò và trạng thái từ database</b>
 * ở mỗi request thay vì tin claim trong token. Nhờ vậy tài khoản bị khóa, bị xóa hoặc bị đổi vai trò mất quyền ngay
 * lập tức, không phải chờ token hết hạn.
 */
@Component
@RequiredArgsConstructor
public class DatabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long userId = parseUserId(jwt.getSubject());
        User user = userRepository.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new InvalidBearerTokenException("Token không còn hiệu lực"));

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .toList();
        return new JwtAuthenticationToken(jwt, authorities, String.valueOf(user.getId()));
    }

    private Long parseUserId(String subject) {
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException | NullPointerException e) {
            throw new InvalidBearerTokenException("Token không hợp lệ");
        }
    }
}
