package org.example.ridehailing.banhangonline.security;

import jakarta.persistence.EntityNotFoundException;
import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private static final Set<String> SUPPORTED_ROLES =
            Set.of("ADMIN", "SHOP_OWNER", "EMPLOYEE", "CUSTOMER");

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Tải thông tin và vai trò mới nhất từ database mỗi lần xác thực username hoặc JWT.
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Email hoặc thông tin xác thực không hợp lệ."));
        String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_ROLES.contains(role)) {
            throw new UsernameNotFoundException("Tài khoản chưa được gán vai trò hợp lệ.");
        }
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_" + role))
                .disabled(!Boolean.TRUE.equals(user.getEnabled()))
                .accountExpired(false)
                .accountLocked(Boolean.TRUE.equals(user.getLocked()))
                .credentialsExpired(false)
                .build();
    }
}
