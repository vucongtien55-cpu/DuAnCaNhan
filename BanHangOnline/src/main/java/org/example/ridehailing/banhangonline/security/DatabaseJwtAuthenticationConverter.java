package org.example.ridehailing.banhangonline.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class DatabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserDetailsService userDetailsService;

    public DatabaseJwtAuthenticationConverter(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    // JWT chỉ giữ subject; quyền được đọc lại từ tài khoản hiện hành thay vì tin claim từ client/token cũ.
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UserDetails user = userDetailsService.loadUserByUsername(jwt.getSubject());
        if (!user.isEnabled()) {
            throw new DisabledException("Tài khoản đã bị vô hiệu hóa.");
        }
        if (!user.isAccountNonLocked()) {
            throw new LockedException("Tài khoản đã bị khóa.");
        }
        return new UsernamePasswordAuthenticationToken(user, jwt, user.getAuthorities());
    }
}
