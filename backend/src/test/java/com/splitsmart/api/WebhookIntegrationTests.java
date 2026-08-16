package com.splitsmart.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitsmart.ingestion.TelegramWebhookPayload;
import com.splitsmart.ingestion.WebhookProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:webhookdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "telegram.bot.secret-token=test_secret_token"
})
@AutoConfigureMockMvc
class WebhookIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WebhookProducer webhookProducer;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    private Set<String> redisKeysStore;

    @BeforeEach
    void setUp() {
        redisKeysStore = new HashSet<>();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Mock SETNX behavior: return true for new key, false if already exists
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    if (redisKeysStore.contains(key)) {
                        return false;
                    }
                    redisKeysStore.add(key);
                    return true;
                });
        
        // Rate limiter increment mock
        when(valueOperations.increment(anyString())).thenReturn(1L);
    }

    @Test
    void testTelegramWebhookIngestionAndDeduplication() throws Exception {
        TelegramWebhookPayload payload = TelegramWebhookPayload.builder()
                .updateId(1001L)
                .chatId("chat_goa_trip")
                .senderId("user_sarah")
                .senderName("Sarah Organizer")
                .messageText("Paid 4000 for dinner at shacks, exclude Maya")
                .timestamp("2026-08-10T19:50:00Z")
                .build();

        // First attempt: should be ACCEPTED
        mockMvc.perform(post("/api/v1/webhooks/telegram")
                        .header("X-Telegram-Bot-Api-Secret-Token", "test_secret_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.idempotencyKey").exists());

        // Second attempt with exact same parameters: should be DUPLICATE_DROPPED
        mockMvc.perform(post("/api/v1/webhooks/telegram")
                        .header("X-Telegram-Bot-Api-Secret-Token", "test_secret_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("DUPLICATE_DROPPED"));
    }

    @Test
    void testTelegramWebhookUnauthorizedSignature() throws Exception {
        TelegramWebhookPayload payload = TelegramWebhookPayload.builder()
                .updateId(1002L)
                .chatId("chat_flatmates")
                .senderId("user_david")
                .messageText("Paid 1500 for WiFi")
                .timestamp("2026-08-10T19:51:00Z")
                .build();

        mockMvc.perform(post("/api/v1/webhooks/telegram")
                        .header("X-Telegram-Bot-Api-Secret-Token", "INVALID_SECRET")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
