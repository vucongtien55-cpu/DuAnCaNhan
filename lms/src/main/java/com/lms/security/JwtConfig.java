package com.lms.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;

@Slf4j
@Configuration
public class JwtConfig {

    static final int MIN_SECRET_BYTES = 32;
    private static final Duration MAX_TTL = Duration.ofHours(24);

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Khóa HMAC-SHA256. Cấu hình sai thì không khởi động (thà dừng còn hơn chạy với khóa yếu); chưa cấu hình thì dùng
     * khóa ngẫu nhiên tạm thời, tuyệt đối không có khóa mặc định trong mã nguồn.
     */
    @Bean
    public SecretKey jwtSecretKey(JwtProperties properties) {
        Duration ttl = properties.accessTokenTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative() || ttl.compareTo(MAX_TTL) > 0) {
            throw new IllegalStateException("app.security.jwt.access-token-ttl phải lớn hơn 0 và không quá 24 giờ");
        }
        byte[] keyBytes;
        if (properties.secret() == null || properties.secret().isBlank()) {
            keyBytes = new byte[MIN_SECRET_BYTES];
            new SecureRandom().nextBytes(keyBytes);
            log.warn("LMS_JWT_SECRET chưa được cấu hình: dùng khóa ký tạm thời, mọi token sẽ mất hiệu lực khi khởi động "
                    + "lại. Hãy đặt LMS_JWT_SECRET (tối thiểu {} byte) khi triển khai.", MIN_SECRET_BYTES);
        } else {
            keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < MIN_SECRET_BYTES) {
                throw new IllegalStateException("LMS_JWT_SECRET phải dài tối thiểu " + MIN_SECRET_BYTES + " byte");
            }
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    /**
     * Kiểm tra chữ ký HS256, hạn dùng (không cho lệch giờ), issuer và việc token đã bị thu hồi hay chưa.
     * Chỉ chấp nhận đúng thuật toán HS256 nên token ký bằng thuật toán khác hoặc {@code alg=none} bị từ chối.
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtProperties properties, Clock clock,
                                 RevokedTokenValidator revokedTokenValidator) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
        timestampValidator.setClock(clock);
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                timestampValidator, new JwtIssuerValidator(properties.issuer()), revokedTokenValidator);
        decoder.setJwtValidator(validator);
        return decoder;
    }
}
