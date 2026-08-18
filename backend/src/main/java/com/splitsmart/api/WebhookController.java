package com.splitsmart.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final IdempotencyService idempotencyService;

    @Value("${splitsmart.webhook.telegram-secret:default-telegram-secret}")
    private String telegramSecret;

    @Value("${splitsmart.webhook.whatsapp-secret:default-whatsapp-secret}")
    private String whatsAppSecret;

    @PostMapping("/telegram")
    public ResponseEntity<Map<String, String>> handleTelegramWebhook(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String signature,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody String payload) {

        // Validate HMAC signature
        if (!validateHmacSignature(payload, signature, telegramSecret)) {
            log.warn("Telegram webhook received with invalid signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid webhook signature"));
        }

        // Idempotency check
        if (idempotencyKey != null && idempotencyService.isDuplicate(idempotencyKey)) {
            log.info("Duplicate Telegram webhook detected for key: {}", idempotencyKey);
            return ResponseEntity.ok(Map.of("status", "already_processed"));
        }

        log.info("Processing Telegram webhook payload");

        // Store idempotency record
        if (idempotencyKey != null) {
            idempotencyService.storeResponse(idempotencyKey, "{\"status\":\"accepted\"}");
        }

        return ResponseEntity.accepted()
                .body(Map.of("status", "accepted", "source", "telegram"));
    }

    @PostMapping("/whatsapp")
    public ResponseEntity<Map<String, String>> handleWhatsAppWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody String payload) {

        // Validate HMAC-SHA256 signature
        if (!validateHmacSignature(payload, signature, whatsAppSecret)) {
            log.warn("WhatsApp webhook received with invalid signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid webhook signature"));
        }

        // Idempotency check
        if (idempotencyKey != null && idempotencyService.isDuplicate(idempotencyKey)) {
            log.info("Duplicate WhatsApp webhook detected for key: {}", idempotencyKey);
            return ResponseEntity.ok(Map.of("status", "already_processed"));
        }

        log.info("Processing WhatsApp webhook payload");

        if (idempotencyKey != null) {
            idempotencyService.storeResponse(idempotencyKey, "{\"status\":\"accepted\"}");
        }

        return ResponseEntity.accepted()
                .body(Map.of("status", "accepted", "source", "whatsapp"));
    }

    @GetMapping("/whatsapp")
    public ResponseEntity<String> verifyWhatsAppWebhook(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {

        if ("subscribe".equals(mode) && whatsAppSecret.equals(verifyToken)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    private boolean validateHmacSignature(String payload, String receivedSignature, String secret) {
        if (receivedSignature == null || receivedSignature.isBlank()) {
            return false;
        }

        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(keySpec);
            byte[] computedHash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = HexFormat.of().formatHex(computedHash);

            // Strip "sha256=" prefix if present (WhatsApp format)
            String cleanReceived = receivedSignature.startsWith("sha256=")
                    ? receivedSignature.substring(7)
                    : receivedSignature;

            return computedSignature.equalsIgnoreCase(cleanReceived);
        } catch (Exception e) {
            log.error("HMAC signature validation failed", e);
            return false;
        }
    }
}
