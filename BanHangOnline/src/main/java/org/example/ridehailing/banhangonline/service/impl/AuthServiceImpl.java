package org.example.ridehailing.banhangonline.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ridehailing.banhangonline.dto.ApiDtoMapper;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.example.ridehailing.banhangonline.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
@Transactional(readOnly = true)

public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;
    private final Duration accessTokenTtl;
    private final String issuer;

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            UserRepository userRepository,
            LoginAttemptService loginAttemptService,
            @Value("${security.jwt.access-token-ttl:PT15M}") Duration accessTokenTtl,
            @Value("${security.jwt.issuer:banhangonline}") String issuer) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
        this.accessTokenTtl = accessTokenTtl;
        this.issuer = issuer;
        if (accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException("Thời hạn access token phải lớn hơn 0.");
        }
    }

    @Override
    public ApiResponseDtos.AuthResponse login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác.");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmail(normalizedEmail)
                .filter(user -> Boolean.TRUE.equals(user.getLocked()))
                .ifPresent(user -> {
                    throw new LockedException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
                });

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, password));
        } catch (BadCredentialsException exception) {
            if (loginAttemptService.recordFailure(normalizedEmail)) {
                throw new LockedException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
            }
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác.");
        }
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(principal.getUsername())
                .filter(account -> Boolean.TRUE.equals(account.getEnabled()))
                .orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không chính xác."));
        if (!loginAttemptService.recordSuccess(user.getId())) {
            throw new LockedException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(accessTokenTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(normalizedEmail)
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new ApiResponseDtos.AuthResponse(
                token, "Bearer", accessTokenTtl.toSeconds(), ApiDtoMapper.toResponse(user));
    }

}
