package org.example.ridehailing.banhangonline;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.exception.DuplicateResourceException;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.example.ridehailing.banhangonline.security.DatabaseJwtAuthenticationConverter;
import org.example.ridehailing.banhangonline.service.AuthService;
import org.example.ridehailing.banhangonline.service.UserService;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class BanHangOnlineApplicationTests {

    private static final String TEST_JWT_SECRET = createTestSecret();

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private DatabaseJwtAuthenticationConverter jwtAuthenticationConverter;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void configureJwtSecret(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.secret-base64", () -> TEST_JWT_SECRET);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void registrationHashesPasswordAndLoginIssuesVerifiableJwt() {
        String email = "jwt-" + java.util.UUID.randomUUID() + "@example.com";
        String rawPassword = "StrongPassword123!";

        User registered = userService.registerCustomer("JWT Test", email, rawPassword, null);

        assertNotEquals(rawPassword, registered.getPasswordHash());
        assertTrue(passwordEncoder.matches(rawPassword, registered.getPasswordHash()));
        assertEquals("CUSTOMER", registered.getRole());

        ApiResponseDtos.AuthResponse response = authService.login(email, rawPassword);
        assertEquals("Bearer", response.tokenType());
        assertEquals(email, jwtDecoder.decode(response.accessToken()).getSubject());
        assertEquals(registered.getId(), userRepository.findByEmail(email).orElseThrow().getId());
    }

    @Test
    void registrationRejectsDuplicateEmailIgnoringCase() {
        String email = "duplicate-" + java.util.UUID.randomUUID() + "@example.com";
        userService.registerCustomer("First User", email, "StrongPassword123!", null);

        assertThrows(DuplicateResourceException.class,
                () -> userService.registerCustomer("Second User", email.toUpperCase(),
                        "StrongPassword123!", null));
    }

    @Test
    void publicCatalogIsOpenAndProtectedEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidRequestBodyReturnsStructuredValidationError() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"","email":"not-an-email","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dữ liệu đầu vào không hợp lệ"))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void missingRequestParameterReturnsBadRequestProblemDetail() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Thiếu tham số"));
    }

    @Test
    void customerJwtCannotAccessAdminOnlyEndpoint() throws Exception {
        String email = "customer-" + java.util.UUID.randomUUID() + "@example.com";
        userService.registerCustomer("JWT Customer", email, "StrongPassword123!", null);
        String token = authService.login(email, "StrongPassword123!").accessToken();

        mockMvc.perform(get("/api/products/low-stock")
                        .param("maximumStock", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/shops/1/staff")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/shops/1/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1}
                                """)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidBearerTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void badCredentialsAreRejectedByLoginEndpoint() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing-%s@example.com","password":"WrongPassword123!"}
                                """.formatted(java.util.UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtUsesLatestDatabaseRoleRatherThanAClientRoleClaim() {
        String email = "role-" + java.util.UUID.randomUUID() + "@example.com";
        User user = userService.registerCustomer("Role Test", email, "StrongPassword123!", null);
        String token = authService.login(email, "StrongPassword123!").accessToken();

        user.setRole("EMPLOYEE");
        userRepository.save(user);

        var authentication = jwtAuthenticationConverter.convert(jwtDecoder.decode(token));
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_EMPLOYEE")));
        assertFalse(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CUSTOMER")));
    }

    private static String createTestSecret() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        return Base64.getEncoder().encodeToString(secret);
    }
}
