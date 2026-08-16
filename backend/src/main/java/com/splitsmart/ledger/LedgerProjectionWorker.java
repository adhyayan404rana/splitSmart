package com.splitsmart.ledger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerProjectionWorker {

    private final GroupBalanceRepository groupBalanceRepository;

    @Transactional
    public void projectCommittedExpense(UUID groupId, UUID payerId, long totalAmountCents, List<UUID> participantIds) {
        log.info("Projecting ExpenseCommitted event for Group={}, Payer={}, AmountCents={}", groupId, payerId, totalAmountCents);

        if (participantIds == null || participantIds.isEmpty()) {
            return;
        }

        // Fowler Money integer arithmetic: calculate per-participant split share
        long splitShareCents = totalAmountCents / participantIds.size();

        // 1. Credit Payer (Total Amount Paid minus Payer's own share if included)
        long payerCreditCents = totalAmountCents - (participantIds.contains(payerId) ? splitShareCents : 0);
        updateUserNetBalance(groupId, payerId, payerCreditCents);

        // 2. Debit Participants (each debited by splitShareCents)
        for (UUID participantId : participantIds) {
            if (!participantId.equals(payerId)) {
                updateUserNetBalance(groupId, participantId, -splitShareCents);
            }
        }
    }

    private void updateUserNetBalance(UUID groupId, UUID userId, long deltaCents) {
        GroupBalanceEntity balanceEntity = groupBalanceRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseGet(() -> GroupBalanceEntity.builder()
                        .groupId(groupId)
                        .userId(userId)
                        .netBalanceCents(0L)
                        .build());

        balanceEntity.setNetBalanceCents(balanceEntity.getNetBalanceCents() + deltaCents);
        groupBalanceRepository.save(balanceEntity);
    }

    @Transactional
    public void recordSettlement(UUID groupId, UUID debtorId, UUID creditorId, long amountCents) {
        log.info("Projecting SettlementMarked event for Group={}, Debtor={}, Creditor={}, AmountCents={}",
                groupId, debtorId, creditorId, amountCents);
        updateUserNetBalance(groupId, debtorId, amountCents);
        updateUserNetBalance(groupId, creditorId, -amountCents);
    }
}
