package org.example.ridehailing.banhangonline.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.validation.Validator;
import org.example.ridehailing.banhangonline.security.DatabaseJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public SecretKey jwtSecretKey(JwtProperties properties, Validator validator) {
        var violations = validator.validate(properties);
        if (!violations.isEmpty()) {
            throw new IllegalStateException("Cấu hình security.jwt không hợp lệ: "
                    + violations.iterator().next().getPropertyPath() + " "
                    + violations.iterator().next().getMessage());
        }
        if (properties.accessTokenTtl().isZero() || properties.accessTokenTtl().isNegative()) {
            throw new IllegalStateException("security.jwt.access-token-ttl phải lớn hơn 0.");
        }

        try {
            byte[] key = Base64.getDecoder().decode(properties.secretBase64());
            if (key.length < 32) {
                throw new IllegalStateException("JWT_SECRET_BASE64 phải giải mã thành ít nhất 32 byte.");
            }
            return new SecretKeySpec(key, "HmacSHA256");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET_BASE64 không phải chuỗi Base64 hợp lệ.", exception);
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DatabaseJwtAuthenticationConverter authenticationConverter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/styles.css",
                                "/app.js", "/api.js", "/services.js", "/favicon.ico")
                        .permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/register", "/api/auth/login",
                                "/api/auth/password-reset-requests", "/api/auth/password-resets").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/users/me/password")
                        .hasAnyRole("ADMIN", "SHOP_OWNER", "EMPLOYEE", "CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/shops/mine").hasAnyRole("CUSTOMER", "SHOP_OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/shops/assigned-to-me").hasRole("EMPLOYEE")
                        // GUEST chỉ có quyền đọc catalog công khai; endpoint low-stock không công khai.
                        .requestMatchers(HttpMethod.GET, "/api/products/low-stock").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,
                                "/api/products", "/api/products/by-price", "/api/products/*",
                                "/api/products/*/reviews", "/api/products/*/reviews/minimum-rating",
                                "/api/categories", "/api/categories/search", "/api/categories/slug/*",
                                "/api/categories/*/children", "/api/categories/*/products",
                                "/api/shops", "/api/shops/*", "/api/shops/*/products")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/shops").permitAll()
                        // Người mua đã đăng nhập được tạo shop; quyền theo từng shop vẫn do service kiểm tra.
                        .requestMatchers(HttpMethod.POST, "/api/shops")
                        .hasAnyRole("CUSTOMER", "SHOP_OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/shops/*")
                        .hasRole("SHOP_OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/shops/*")
                        .hasRole("SHOP_OWNER")
                        // Chỉ chủ shop mới được cấp/ngừng tư cách nhân viên hoặc ghi giao dịch shop.
                        .requestMatchers(HttpMethod.POST, "/api/shops/*/staff/**",
                                "/api/shops/*/transactions")
                        .hasRole("SHOP_OWNER")
                        .requestMatchers("/api/shops/*/transactions/**")
                        .hasRole("SHOP_OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/shops/*/staff/**")
                        .hasRole("SHOP_OWNER")
                        // Chủ shop và nhân viên được thao tác nghiệp vụ trong shop; service xác minh đúng shop.
                        .requestMatchers("/api/shops/*/staff",
                                "/api/shops/*/customers/**",
                                "/api/shops/*/customer-notes",
                                "/api/shops/*/customer-notes/**",
                                "/api/shops/*/appointments",
                                "/api/shops/*/orders/**",
                                "/api/shops/*/transactions/**")
                        .hasAnyRole("SHOP_OWNER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.POST, "/api/shops/*/products")
                        .hasRole("SHOP_OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/shops/*/manage/products")
                        .hasAnyRole("SHOP_OWNER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**")
                        .hasRole("SHOP_OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                        .hasRole("SHOP_OWNER")
                        // Chỉ ADMIN được quản lý danh mục và truy cập vùng quản trị.
                        .requestMatchers("/api/admin/**", "/api/categories/**")
                        .hasRole("ADMIN")
                        // Hồ sơ, giỏ hàng, đơn và thanh toán là nghiệp vụ khách đã đăng nhập.
                        .requestMatchers("/api/users/me", "/api/users/me/reviews",
                                "/api/cart/**", "/api/orders/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products/*/reviews")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/reviews/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpStatus.FORBIDDEN.value())));
        return http.build();
    }
}
