package com.splitsmart.api;

import com.splitsmart.ledger.EventAuditResponse;
import com.splitsmart.ledger.GroupBalanceResponse;
import com.splitsmart.ledger.LedgerCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerCommandService ledgerCommandService;

    @GetMapping("/groups/{groupId}/balances")
    public ResponseEntity<List<GroupBalanceResponse>> getGroupBalances(@PathVariable UUID groupId) {
        return ResponseEntity.ok(ledgerCommandService.getGroupBalances(groupId));
    }

    @GetMapping("/expenses/{id}/history")
    public ResponseEntity<List<EventAuditResponse>> getEventAuditHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ledgerCommandService.getEventAuditLog(id));
    }
}
