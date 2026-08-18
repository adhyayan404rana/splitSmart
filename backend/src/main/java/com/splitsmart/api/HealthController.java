package com.splitsmart.api;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("service", "splitsmart-backend");
        health.put("timestamp", Instant.now().toString());

        // Database connectivity check
        health.put("database", checkDatabaseHealth());

        // Redis connectivity check
        health.put("redis", checkRedisHealth());

        boolean allHealthy = "UP".equals(health.get("database")) && "UP".equals(health.get("redis"));
        health.put("status", allHealthy ? "UP" : "DEGRADED");

        return ResponseEntity.ok(health);
    }

    @GetMapping("/liveness")
    public ResponseEntity<Map<String, String>> liveness() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> readiness = new LinkedHashMap<>();
        String dbStatus = checkDatabaseHealth();
        readiness.put("database", dbStatus);
        readiness.put("status", "UP".equals(dbStatus) ? "READY" : "NOT_READY");
        return ResponseEntity.ok(readiness);
    }

    private String checkDatabaseHealth() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(3)) {
                return "UP";
            }
            return "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String checkRedisHealth() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            return pong != null ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
