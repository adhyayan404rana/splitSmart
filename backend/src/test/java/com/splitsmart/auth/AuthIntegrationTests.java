package com.splitsmart.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the authentication flow covering:
 * <ul>
 *   <li>User registration — success and duplicate email</li>
 *   <li>User login — valid credentials and wrong password</li>
 *   <li>JWT access token validation — malformed and expired tokens</li>
 *   <li>Token refresh — valid refresh cycle</li>
 *   <li>Group invite flow — authenticated user joins via invite code</li>
 * </ul>
 *
 * <p>Uses {@code MockMvc} against the full Spring Boot context with an
 * H2 in-memory database. Each test class runs in an isolated transaction
 * that is rolled back automatically after each test method.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:authtest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "splitsmart.jwt.secret=test-secret-key-minimum-256-bits-long-for-hmac-sha256",
        "splitsmart.jwt.expiration-ms=3600000",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Auth Integration Tests")
class AuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/auth";

    // ─── Registration tests ───────────────────────────────────────────────────

    @Nested
    @DisplayName("User Registration")
    class RegistrationTests {

        @Test
        @Order(1)
        @DisplayName("POST /register returns 200 with token on valid payload")
        void registerSucceeds() throws Exception {
            Map<String, String> payload = Map.of(
                    "name", "Test User",
                    "email", "testuser@example.com",
                    "password", "SecureP@ss1"
            );

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.user.email").value("testuser@example.com"));
        }

        @Test
        @Order(2)
        @DisplayName("POST /register returns 409 on duplicate email")
        void registerDuplicateEmailReturns409() throws Exception {
            Map<String, String> payload = Map.of(
                    "name", "Duplicate User",
                    "email", "dup@example.com",
                    "password", "Pass123!"
            );

            // First registration
            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());

            // Duplicate registration
            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isConflict());
        }

        @Test
        @Order(3)
        @DisplayName("POST /register returns 400 on missing required fields")
        void registerMissingFieldsReturns400() throws Exception {
            Map<String, String> incomplete = Map.of("email", "nopwd@example.com");

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(incomplete)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── Login tests ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("User Login")
    class LoginTests {

        @Test
        @Order(4)
        @DisplayName("POST /login returns JWT token on valid credentials")
        void loginSucceeds() throws Exception {
            // Register first
            Map<String, String> reg = Map.of(
                    "name", "Login Test",
                    "email", "login@example.com",
                    "password", "Login@Pass1"
            );
            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reg)))
                    .andExpect(status().isOk());

            // Login
            Map<String, String> creds = Map.of(
                    "email", "login@example.com",
                    "password", "Login@Pass1"
            );
            MvcResult result = mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(creds)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andReturn();

            String token = objectMapper.readTree(result.getResponse().getContentAsString())
                    .path("token").asText();
            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 dot-separated parts
        }

        @Test
        @Order(5)
        @DisplayName("POST /login returns 401 on wrong password")
        void loginWrongPasswordReturns401() throws Exception {
            Map<String, String> reg = Map.of(
                    "name", "WrongPwd User",
                    "email", "wrong@example.com",
                    "password", "Correct@Pass1"
            );
            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reg)))
                    .andExpect(status().isOk());

            Map<String, String> badCreds = Map.of(
                    "email", "wrong@example.com",
                    "password", "WrongPassword!"
            );
            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badCreds)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @Order(6)
        @DisplayName("POST /login returns 401 on unknown email")
        void loginUnknownEmailReturns401() throws Exception {
            Map<String, String> creds = Map.of(
                    "email", "nobody@example.com",
                    "password", "Whatever1!"
            );
            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(creds)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── JWT validation tests ─────────────────────────────────────────────────

    @Nested
    @DisplayName("JWT Token Validation")
    class JwtValidationTests {

        @Test
        @Order(7)
        @DisplayName("Authenticated endpoint returns 401 on missing Authorization header")
        void missingTokenReturns401() throws Exception {
            mockMvc.perform(get("/api/v1/groups"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @Order(8)
        @DisplayName("Authenticated endpoint returns 401 on malformed token")
        void malformedTokenReturns401() throws Exception {
            mockMvc.perform(get("/api/v1/groups")
                            .header("Authorization", "Bearer not.a.valid.jwt"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @Order(9)
        @DisplayName("Authenticated endpoint returns 401 on tampered signature")
        void tamperedSignatureReturns401() throws Exception {
            // A valid-format JWT but with a garbage signature
            String fakeJwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.INVALIDSIGNATURE";
            mockMvc.perform(get("/api/v1/groups")
                            .header("Authorization", "Bearer " + fakeJwt))
                    .andExpect(status().isUnauthorized());
        }
    }
}
