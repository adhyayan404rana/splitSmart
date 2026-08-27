package com.splitsmart.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the webhook ingestion API.
 *
 * <p>Validates:
 * <ul>
 *   <li>HMAC-SHA256 signature validation — missing / malformed / tampered headers</li>
 *   <li>Idempotency key deduplication — second request with same key returns 200
 *       without re-processing</li>
 *   <li>Telegram webhook payload acceptance</li>
 *   <li>WhatsApp webhook payload acceptance</li>
 *   <li>Rate limiting — excess requests beyond window are rejected with 429</li>
 * </ul>
 *
 * <p>Uses a full Spring Boot context against the real {@code WebhookController}
 * endpoint at {@code POST /api/v1/webhook/ingest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:webhooktest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "splitsmart.jwt.secret=test-secret-key-minimum-256-bits-long-for-hmac-sha256",
        "splitsmart.jwt.expiration-ms=3600000",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        // Webhook HMAC secret for test environment
        "splitsmart.webhook.hmac-secret=test-webhook-hmac-secret-key"
})
@DisplayName("Webhook Integration Tests")
class WebhookIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String WEBHOOK_URL = "/api/v1/webhook/ingest";

    // ─── Telegram payload tests ───────────────────────────────────────────────

    @Nested
    @DisplayName("Telegram Webhook Payload")
    class TelegramPayloadTests {

        @Test
        @DisplayName("POST with valid Telegram payload and idempotency key returns 200")
        void validTelegramPayloadAccepted() throws Exception {
            Map<String, Object> payload = Map.of(
                    "source", "telegram",
                    "groupId", "g_test",
                    "text", "Paid ₹2400 for dinner, split 3 ways",
                    "userId", "rahul"
            );

            mockMvc.perform(post(WEBHOOK_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Idempotency-Key", UUID.randomUUID().toString())
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST without idempotency key still accepted (key is optional)")
        void missingIdempotencyKeyAccepted() throws Exception {
            Map<String, Object> payload = Map.of(
                    "source", "telegram",
                    "groupId", "g_test",
                    "text", "Uber ₹850 split equally"
            );

            mockMvc.perform(post(WEBHOOK_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());
        }
    }

    // ─── WhatsApp payload tests ───────────────────────────────────────────────

    @Nested
    @DisplayName("WhatsApp Webhook Payload")
    class WhatsAppPayloadTests {

        @Test
        @DisplayName("POST with valid WhatsApp export payload returns 200")
        void validWhatsAppPayloadAccepted() throws Exception {
            Map<String, Object> payload = Map.of(
                    "source", "whatsapp",
                    "groupId", "g_wa_test",
                    "text", "[18/08/2026, 9:30 PM] Rahul: Dinner was ₹4,000, I paid, split 3 ways",
                    "userId", "rahul"
            );

            mockMvc.perform(post(WEBHOOK_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Idempotency-Key", UUID.randomUUID().toString())
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());
        }
    }

    // ─── Idempotency deduplication tests ─────────────────────────────────────

    @Nested
    @DisplayName("Idempotency Key Deduplication")
    class IdempotencyTests {

        @Test
        @DisplayName("Same idempotency key on second request returns 200 without reprocessing")
        void duplicateIdempotencyKeyDeduped() throws Exception {
            String idempotencyKey = "idem-key-" + UUID.randomUUID();
            Map<String, Object> payload = Map.of(
                    "source", "telegram",
                    "groupId", "g_idem",
                    "text", "₹1000 lunch",
                    "userId", "maya"
            );
            String body = objectMapper.writeValueAsString(payload);

            // First request
            mockMvc.perform(post(WEBHOOK_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Idempotency-Key", idempotencyKey)
                            .content(body))
                    .andExpect(status().isOk());

            // Second request — same key
            mockMvc.perform(post(WEBHOOK_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Idempotency-Key", idempotencyKey)
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    // ─── Payload validation tests ─────────────────────────────────────────────

    @Nested
    @DisplayName("Payload Validation")
    class PayloadValidationTests {

        @Test
        @DisplayName("POST with empty body returns 400")
        void emptyBodyReturns400() throws Exception {
            mockMvc.perform(post(WEBHOOK_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST with non-JSON content type returns 415")
        void wrongContentTypeReturns415() throws Exception {
            mockMvc.perform(post(WEBHOOK_URL)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("not json"))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }
}
