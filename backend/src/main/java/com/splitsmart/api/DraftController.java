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
@RequestMapping("/api/v1/drafts")
@RequiredArgsConstructor
public class DraftController {

    @GetMapping("/group/{groupId}")
    public ResponseEntity<Map<String, Object>> getGroupDrafts(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(Map.of(
                "groupId", groupId.toString(),
                "drafts", List.of(),
                "total", 0
        ));
    }

    @GetMapping("/{draftId}")
    public ResponseEntity<Map<String, Object>> getDraftById(
            @PathVariable UUID draftId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(Map.of(
                "draftId", draftId.toString(),
                "status", "PENDING_REVIEW",
                "message", "Draft entity pending ledger integration"
        ));
    }

    @PostMapping("/{draftId}/approve")
    public ResponseEntity<Map<String, String>> approveDraft(
            @PathVariable UUID draftId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(Map.of(
                "draftId", draftId.toString(),
                "action", "approved",
                "approvedBy", userPrincipal.getId().toString()
        ));
    }

    @PostMapping("/{draftId}/dispute")
    public ResponseEntity<Map<String, String>> disputeDraft(
            @PathVariable UUID draftId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }

        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "No reason provided";

        return ResponseEntity.ok(Map.of(
                "draftId", draftId.toString(),
                "action", "disputed",
                "disputedBy", userPrincipal.getId().toString(),
                "reason", reason
        ));
    }
}
