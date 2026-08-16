package com.splitsmart.api;

import com.splitsmart.ledger.GroupBalanceEntity;
import com.splitsmart.ledger.GroupBalanceRepository;
import com.splitsmart.settlement.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/debts")
@RequiredArgsConstructor
public class DebtController {

    private final DebtSimplificationEngine debtSimplificationEngine;
    private final GroupBalanceRepository groupBalanceRepository;

    @GetMapping("/groups/{groupId}/simplified")
    public ResponseEntity<List<SimplifiedDebtResponse>> getSimplifiedDebts(@PathVariable UUID groupId) {
        List<GroupBalanceEntity> balances = groupBalanceRepository.findByGroupId(groupId);
        Map<UUID, Long> netMap = new HashMap<>();
        Map<UUID, String> userNames = new HashMap<>();

        for (GroupBalanceEntity b : balances) {
            netMap.put(b.getUserId(), b.getNetBalanceCents());
            userNames.put(b.getUserId(), "Member " + b.getUserId().toString().substring(0, 4));
        }

        return ResponseEntity.ok(debtSimplificationEngine.simplifyDebts(netMap, userNames));
    }

    @GetMapping("/groups/{groupId}/raw")
    public ResponseEntity<List<RawDebtResponse>> getRawDebts(@PathVariable UUID groupId) {
        List<RawDebtResponse> sampleRawDebts = createSampleRawDebts(groupId);
        return ResponseEntity.ok(sampleRawDebts);
    }

    @GetMapping("/groups/{groupId}/comparison")
    public ResponseEntity<DebtGraphComparisonResponse> getDebtComparison(@PathVariable UUID groupId) {
        List<RawDebtResponse> sampleRawDebts = createSampleRawDebts(groupId);
        Map<UUID, String> names = Map.of(
                UUID.fromString("11111111-1111-1111-1111-111111111111"), "Sarah",
                UUID.fromString("22222222-2222-2222-2222-222222222222"), "Rahul",
                UUID.fromString("33333333-3333-3333-3333-333333333333"), "Amit",
                UUID.fromString("44444444-4444-4444-4444-444444444444"), "David"
        );
        return ResponseEntity.ok(debtSimplificationEngine.computeComparison(sampleRawDebts, names));
    }

    private List<RawDebtResponse> createSampleRawDebts(UUID groupId) {
        UUID u1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID u2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID u3 = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID u4 = UUID.fromString("44444444-4444-4444-4444-444444444444");

        return List.of(
                new RawDebtResponse(u2, "Rahul", u1, "Sarah", 150000L, "INR"),
                new RawDebtResponse(u3, "Amit", u2, "Rahul", 150000L, "INR"),
                new RawDebtResponse(u1, "Sarah", u3, "Amit", 150000L, "INR"), // Circular cycle A->B->C->A
                new RawDebtResponse(u4, "David", u1, "Sarah", 200000L, "INR"),
                new RawDebtResponse(u3, "Amit", u4, "David", 100000L, "INR"),
                new RawDebtResponse(u2, "Rahul", u4, "David", 100000L, "INR")
        );
    }
}
