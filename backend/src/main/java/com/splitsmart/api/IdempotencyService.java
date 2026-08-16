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
    private static final Duration TTL_48_HOURS = Duration.ofHours(48);

    public String generateIdempotencyKey(String chatId, String timestamp, String payerId) {
        String raw = (chatId != null ? chatId : "") + ":" + 
                     (timestamp != null ? timestamp : "") + ":" + 
                     (payerId != null ? payerId : "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Tries to acquire an idempotency lock in Redis for 48 hours.
     * @return true if key is NEW (lock acquired), false if key ALREADY EXISTS (duplicate)
     */
    public boolean acquireIdempotencyKey(String key) {
        if (key == null || key.isBlank()) return true;

        String redisKey = IDEMPOTENCY_PREFIX + key;
        Boolean isNewKey = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSED", TTL_48_HOURS);
        return Boolean.TRUE.equals(isNewKey);
    }
}
