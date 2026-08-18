package com.splitsmart.api;

import com.splitsmart.auth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settlement/debts")
@RequiredArgsConstructor
public class DebtController {

    @GetMapping("/group/{groupId}")
    public ResponseEntity<Map<String, Object>> getGroupDebts(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }

        // Placeholder response until SettlementService is wired in Day 12
        return ResponseEntity.ok(Map.of(
                "groupId", groupId.toString(),
                "debts", List.of(),
                "simplified", false,
                "message", "Settlement engine pending integration"
        ));
    }

    @GetMapping("/group/{groupId}/simplified")
    public ResponseEntity<Map<String, Object>> getSimplifiedDebts(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(Map.of(
                "groupId", groupId.toString(),
                "debts", List.of(),
                "simplified", true,
                "message", "Simplified debt graph pending integration"
        ));
    }

    @GetMapping("/group/{groupId}/comparison")
    public ResponseEntity<Map<String, Object>> getDebtComparison(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(Map.of(
                "groupId", groupId.toString(),
                "rawDebts", List.of(),
                "simplifiedDebts", List.of(),
                "reductionPercentage", 0
        ));
    }
}
