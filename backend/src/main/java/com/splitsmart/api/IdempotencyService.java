package com.splitsmart.api;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENCY_PREFIX = "splitsmart:idempotency:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    /**
     * Checks whether a request with the given idempotency key has already been processed.
     * Returns true if this is a duplicate request, false if it's the first occurrence.
     */
    public boolean isDuplicate(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }

        String hashedKey = hashKey(idempotencyKey);
        String redisKey = IDEMPOTENCY_PREFIX + hashedKey;

        Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", DEFAULT_TTL);
        return wasAbsent == null || !wasAbsent;
    }

    /**
     * Stores a response payload against an idempotency key so that
     * duplicate requests can return the same response.
     */
    public void storeResponse(String idempotencyKey, String responsePayload) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        String hashedKey = hashKey(idempotencyKey);
        String responseKey = IDEMPOTENCY_PREFIX + hashedKey + ":response";
        redisTemplate.opsForValue().set(responseKey, responsePayload, DEFAULT_TTL);
    }

    /**
     * Retrieves a previously stored response for the given idempotency key.
     */
    public String getStoredResponse(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        String hashedKey = hashKey(idempotencyKey);
        String responseKey = IDEMPOTENCY_PREFIX + hashedKey + ":response";
        return redisTemplate.opsForValue().get(responseKey);
    }

    /**
     * Invalidates (removes) the idempotency key and its stored response.
     */
    public void invalidate(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        String hashedKey = hashKey(idempotencyKey);
        redisTemplate.delete(IDEMPOTENCY_PREFIX + hashedKey);
        redisTemplate.delete(IDEMPOTENCY_PREFIX + hashedKey + ":response");
    }

    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
