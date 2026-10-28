package com.lms.security;

import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.entity.enums.RoleName;
import com.lms.repository.RevokedTokenRepository;
import com.lms.security.JwtTokenService.IssuedToken;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtConfigTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    /** Đồng hồ có thể tua để kiểm tra hết hạn mà không phải chờ. */
    private static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final JwtConfig config = new JwtConfig();
    private MutableClock clock;
    private JwtProperties properties;
    private SecretKey key;
    private RevokedTokenRepository revokedRepository;
    private JwtDecoder decoder;
    private JwtTokenService tokenService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        properties = new JwtProperties(SECRET, Duration.ofMinutes(30), "lms");
        key = config.jwtSecretKey(properties);
        revokedRepository = mock(RevokedTokenRepository.class);
        decoder = config.jwtDecoder(key, properties, clock, new RevokedTokenValidator(revokedRepository));
        tokenService = new JwtTokenService(config.jwtEncoder(key), properties, clock);
    }

    private User user() {
        Role role = new Role();
        role.setName(RoleName.LECTURER);
        User user = new User();
        user.setId(42L);
        user.setUsername("gv01");
        user.setRoles(Set.of(role));
        return user;
    }

    // ---------- cấu hình khóa ----------

    @Test
    @DisplayName("Khóa cấu hình đủ 32 byte được dùng làm khóa HMAC-SHA256")
    void configuredSecret() {
        assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(key.getEncoded()).isEqualTo(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Khóa ngắn hơn 32 byte -> không khởi động")
    void secretTooShort() {
        assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties("ngan", Duration.ofHours(1), "lms")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    @DisplayName("Chưa cấu hình khóa: dùng khóa ngẫu nhiên tạm thời, mỗi lần khác nhau (không có khóa mặc định)")
    void ephemeralSecretWhenMissing() {
        SecretKey first = config.jwtSecretKey(new JwtProperties("", Duration.ofHours(1), "lms"));
        SecretKey second = config.jwtSecretKey(new JwtProperties(null, Duration.ofHours(1), "lms"));

        assertThat(first.getEncoded()).hasSize(32).isNotEqualTo(second.getEncoded());
    }

    @Test
    @DisplayName("Thời hạn token không hợp lệ (0, âm, quá 24 giờ) -> không khởi động")
    void invalidTtl() {
        for (Duration ttl : new Duration[]{Duration.ZERO, Duration.ofMinutes(-5), Duration.ofHours(25)}) {
            assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties(SECRET, ttl, "lms")))
                    .as("ttl " + ttl)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    @DisplayName("Khóa ký không bao giờ xuất hiện trong toString của cấu hình")
    void propertiesToStringMasksSecret() {
        assertThat(properties.toString()).doesNotContain(SECRET);
    }

    // ---------- phát hành và kiểm tra token ----------

    @Test
    @DisplayName("Token hợp lệ: giải mã được, có đủ claim và hạn đúng theo cấu hình")
    void validToken() {
        IssuedToken issued = tokenService.issue(user());

        Jwt jwt = decoder.decode(issued.value());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("username")).isEqualTo("gv01");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("LECTURER");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("lms");
        assertThat(jwt.getId()).isEqualTo(issued.jti()).isNotBlank();
        assertThat(jwt.getExpiresAt()).isEqualTo(issued.expiresAt());
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(30));
        assertThat(issued.toString()).doesNotContain(issued.value());
    }

    @Test
    @DisplayName("Mỗi token có jti riêng")
    void uniqueJti() {
        assertThat(tokenService.issue(user()).jti()).isNotEqualTo(tokenService.issue(user()).jti());
    }

    @Test
    @DisplayName("Token hết hạn bị từ chối, không có thời gian ân hạn")
    void expiredToken() {
        String token = tokenService.issue(user()).value();

        clock.advance(Duration.ofMinutes(30).plusSeconds(1));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token còn hạn đến sát giờ hết hạn vẫn dùng được")
    void tokenValidUntilExpiry() {
        String token = tokenService.issue(user()).value();

        clock.advance(Duration.ofMinutes(29));

        assertThat(decoder.decode(token).getSubject()).isEqualTo("42");
    }

    @Test
    @DisplayName("Token bị sửa nội dung (chữ ký không còn khớp) bị từ chối")
    void tamperedToken() {
        String token = tokenService.issue(user()).value();
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                        .replace("\"42\"", "\"1\"").getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> decoder.decode(parts[0] + "." + forgedPayload + "." + parts[2]))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token ký bằng khóa khác bị từ chối")
    void wrongKey() {
        SecretKey otherKey = new SecretKeySpec("ffffffffffffffffffffffffffffffff".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        String token = new JwtTokenService(new NimbusJwtEncoder(new ImmutableSecret<>(otherKey)), properties, clock)
                .issue(user()).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token không ký (alg=none) bị từ chối")
    void unsignedToken() {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(("{\"sub\":\"1\",\"iss\":\"lms\",\"jti\":\"x\",\"exp\":"
                + clock.instant().plusSeconds(600).getEpochSecond() + "}").getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> decoder.decode(header + "." + payload + ".")).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token từ nơi phát hành khác (issuer sai) bị từ chối")
    void wrongIssuer() {
        JwtEncoder encoder = config.jwtEncoder(key);
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("ben-khac").subject("42").id("j1")
                .issuedAt(clock.instant()).expiresAt(clock.instant().plusSeconds(600)).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token đã bị thu hồi (đăng xuất) bị từ chối dù chữ ký và hạn còn hợp lệ")
    void revokedToken() {
        IssuedToken issued = tokenService.issue(user());
        when(revokedRepository.existsByJti(issued.jti())).thenReturn(true);

        assertThatThrownBy(() -> decoder.decode(issued.value())).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Chuỗi rác không phải JWT bị từ chối")
    void garbageToken() {
        assertThatThrownBy(() -> decoder.decode("khong-phai-jwt")).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Thuộc tính CORS không cho phép '*'")
    void corsRejectsWildcard() {
        assertThatThrownBy(() -> new CorsProperties(java.util.List.of("*"))).isInstanceOf(IllegalStateException.class);
        assertThat(new CorsProperties(java.util.List.of(" http://localhost:3000 ", "")).allowedOrigins())
                .containsExactly("http://localhost:3000");
        assertThat(new CorsProperties(null).allowedOrigins()).isEmpty();
    }
}
