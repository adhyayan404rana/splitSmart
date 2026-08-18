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
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {

    @GetMapping("/group/{groupId}/events")
    public ResponseEntity<Map<String, Object>> getGroupEvents(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(Map.of(
                "groupId", groupId.toString(),
                "events", List.of(),
                "totalEvents", 0,
                "message", "Event store pending integration"
        ));
    }

    @GetMapping("/group/{groupId}/balances")
    public ResponseEntity<Map<String, Object>> getGroupBalances(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(Map.of(
                "groupId", groupId.toString(),
                "balances", List.of(),
                "lastUpdated", java.time.Instant.now().toString()
        ));
    }

    @GetMapping("/group/{groupId}/audit")
    public ResponseEntity<Map<String, Object>> getAuditTrail(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(Map.of(
                "groupId", groupId.toString(),
                "auditEntries", List.of(),
                "message", "Audit trail pending event store integration"
        ));
    }

    @GetMapping("/group/{groupId}/consensus")
    public ResponseEntity<Map<String, Object>> getConsensusStatus(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(Map.of(
                "groupId", groupId.toString(),
                "pendingApprovals", 0,
                "approvedEvents", 0,
                "disputedEvents", 0
        ));
    }
}
