package com.splitsmart.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping({"/", "/api/v1/health"})
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "splitsmart-backend",
            "message", "SplitSmart Backend Spring Boot API Server is running",
            "timestamp", Instant.now().toString(),
            "version", "0.0.1-SNAPSHOT"
        ));
    }
}
