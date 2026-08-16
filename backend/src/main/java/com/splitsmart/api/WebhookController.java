package com.splitsmart.api;

import com.splitsmart.ingestion.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final IdempotencyService idempotencyService;
    private final WebhookProducer webhookProducer;

    @Value("${telegram.bot.secret-token:splitsmart_telegram_secret}")
    private String expectedTelegramToken;

    @PostMapping("/telegram")
    public ResponseEntity<WebhookIngestResponse> handleTelegramWebhook(
            @RequestHeader(name = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken,
            @RequestBody TelegramWebhookPayload payload) {

        // Signature Validation (if header provided)
        if (secretToken != null && !secretToken.equals(expectedTelegramToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(WebhookIngestResponse.builder()
                            .status("REJECTED")
                            .message("Invalid Telegram secret token signature")
                            .timestamp(Instant.now().toString())
                            .build());
        }

        // Generate SHA256 Idempotency Key
        String idempotencyKey = idempotencyService.generateIdempotencyKey(
                payload.getChatId(),
                payload.getTimestamp(),
                payload.getSenderId()
        );

        // 48-Hour Redis Atomic SETNX Deduplication
        boolean lockAcquired = idempotencyService.acquireIdempotencyKey(idempotencyKey);
        if (!lockAcquired) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(WebhookIngestResponse.builder()
                            .status("DUPLICATE_DROPPED")
                            .message("Duplicate webhook payload detected; silently dropped")
                            .idempotencyKey(idempotencyKey)
                            .timestamp(Instant.now().toString())
                            .build());
        }

        // Enqueue to RabbitMQ ingestion queue
        webhookProducer.enqueueWebhookPayload(payload);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(WebhookIngestResponse.builder()
                        .status("ACCEPTED")
                        .message("Webhook payload accepted for asynchronous NLP ingestion")
                        .idempotencyKey(idempotencyKey)
                        .timestamp(Instant.now().toString())
                        .build());
    }

    @PostMapping("/whatsapp")
    public ResponseEntity<WebhookIngestResponse> handleWhatsAppWebhook(
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody WhatsAppWebhookPayload payload) {

        String idempotencyKey = idempotencyService.generateIdempotencyKey(
                payload.getGroupId(),
                payload.getTimestamp(),
                payload.getSenderPhone()
        );

        boolean lockAcquired = idempotencyService.acquireIdempotencyKey(idempotencyKey);
        if (!lockAcquired) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(WebhookIngestResponse.builder()
                            .status("DUPLICATE_DROPPED")
                            .message("Duplicate WhatsApp webhook payload detected; silently dropped")
                            .idempotencyKey(idempotencyKey)
                            .timestamp(Instant.now().toString())
                            .build());
        }

        webhookProducer.enqueueWebhookPayload(payload);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(WebhookIngestResponse.builder()
                        .status("ACCEPTED")
                        .message("WhatsApp payload accepted for asynchronous NLP ingestion")
                        .idempotencyKey(idempotencyKey)
                        .timestamp(Instant.now().toString())
                        .build());
    }
}
