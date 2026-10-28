package com.lms.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    static final String LOGIN_PATH = "/api/auth/login";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   DatabaseJwtAuthenticationConverter authenticationConverter,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                // CSRF chỉ nhắm vào xác thực tự động của trình duyệt (cookie/session). API này hoàn toàn stateless và
                // chỉ chấp nhận token trong header Authorization do frontend gắn tay: không có session, không có cookie
                // xác thực nên không có gì để giả mạo. Nếu sau này đưa token vào cookie thì phải bật lại CSRF.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.POST, LOGIN_PATH).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .bearerTokenResolver(bearerTokenResolver())
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter)));
        return http.build();
    }

    /**
     * Chỉ đọc token từ header Authorization (không nhận qua query string hay form để token không lọt vào log/URL).
     * Bỏ qua token trên đường dẫn đăng nhập để token cũ còn sót ở frontend không làm hỏng việc đăng nhập lại.
     */
    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        // Mặc định chỉ đọc "Authorization: Bearer <token>"; không nhận token qua query string hay form.
        DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
        headerResolver.setAllowUriQueryParameter(false);
        headerResolver.setAllowFormEncodedBodyParameter(false);
        return (HttpServletRequest request) ->
                LOGIN_PATH.equals(request.getRequestURI()) ? null : headerResolver.resolve(request);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
