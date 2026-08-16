package com.splitsmart.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testRegisterLoginAndProfileFlow() throws Exception {
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setEmail("sarah@example.com");
        registerReq.setFullName("Sarah Organizer");
        registerReq.setPhone("+919876543210");
        registerReq.setPassword("SecurePass123!");

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse regAuth = objectMapper.readValue(registerResult.getResponse().getContentAsString(), AuthResponse.class);
        assertNotNull(regAuth.getAccessToken());
        assertEquals("sarah@example.com", regAuth.getUser().getEmail());

        // Verify password is stored hashed
        UserEntity dbUser = userRepository.findByEmail("sarah@example.com").orElseThrow();
        assertNotEquals("SecurePass123!", dbUser.getPasswordHash());

        // Login Test
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("sarah@example.com");
        loginReq.setPassword("SecurePass123!");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse loginAuth = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);
        assertNotNull(loginAuth.getAccessToken());

        // Access Protected Profile Endpoint /api/v1/auth/me
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + loginAuth.getAccessToken()))
                .andExpect(status().isOk());
    }
}
