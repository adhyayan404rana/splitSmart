package com.splitsmart.api;

import com.splitsmart.auth.UserPrincipal;
import com.splitsmart.ingestion.ExpenseDraft;
import com.splitsmart.ledger.DraftResponse;
import com.splitsmart.ledger.LedgerCommandService;
import com.splitsmart.ledger.ModifyDraftRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drafts")
@RequiredArgsConstructor
public class DraftController {

    private final LedgerCommandService ledgerCommandService;

    @PostMapping("/group/{groupId}")
    public ResponseEntity<DraftResponse> createDraft(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                     @PathVariable UUID groupId,
                                                     @RequestBody ExpenseDraft input) {
        UUID payerId = userPrincipal != null ? userPrincipal.getId() : UUID.randomUUID();
        String payerName = userPrincipal != null ? userPrincipal.getFullName() : "Payer";
        return ResponseEntity.ok(ledgerCommandService.createDraft(groupId, payerId, payerName, input));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DraftResponse> getDraft(@PathVariable UUID id) {
        return ResponseEntity.ok(ledgerCommandService.getDraft(id));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<DraftResponse>> getGroupDrafts(@PathVariable UUID groupId) {
        return ResponseEntity.ok(ledgerCommandService.getGroupDrafts(groupId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DraftResponse> modifyDraft(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                     @PathVariable UUID id,
                                                     @Valid @RequestBody ModifyDraftRequest request) {
        UUID modifierId = userPrincipal != null ? userPrincipal.getId() : UUID.randomUUID();
        return ResponseEntity.ok(ledgerCommandService.modifyDraft(id, modifierId, request));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<DraftResponse> approveDraft(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                      @PathVariable UUID id) {
        UUID approverId = userPrincipal != null ? userPrincipal.getId() : UUID.randomUUID();
        return ResponseEntity.ok(ledgerCommandService.approveDraft(id, approverId));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<DraftResponse> rejectDraft(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                     @PathVariable UUID id) {
        UUID rejecterId = userPrincipal != null ? userPrincipal.getId() : UUID.randomUUID();
        return ResponseEntity.ok(ledgerCommandService.rejectDraft(id, rejecterId));
    }

    @PostMapping("/{id}/dispute")
    public ResponseEntity<DraftResponse> disputeDraft(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                      @PathVariable UUID id,
                                                      @Valid @RequestBody com.splitsmart.ledger.DisputeRequest request) {
        UUID userId = userPrincipal != null ? userPrincipal.getId() : UUID.randomUUID();
        return ResponseEntity.ok(ledgerCommandService.disputeDraft(id, userId, request));
    }

    @PostMapping("/{id}/resolve-dispute")
    public ResponseEntity<DraftResponse> resolveDispute(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                         @PathVariable UUID id) {
        UUID userId = userPrincipal != null ? userPrincipal.getId() : UUID.randomUUID();
        return ResponseEntity.ok(ledgerCommandService.resolveDispute(id, userId));
    }

    @GetMapping("/{id}/dedup-check")
    public ResponseEntity<com.splitsmart.ledger.DeduplicationWarningResponse> checkDeduplication(@PathVariable UUID id) {
        return ResponseEntity.ok(ledgerCommandService.checkDeduplication(id));
    }
}
