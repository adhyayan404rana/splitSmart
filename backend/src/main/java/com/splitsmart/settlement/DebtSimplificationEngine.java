package com.splitsmart.settlement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class DebtSimplificationEngine {

    @Data
    @AllArgsConstructor
    private static class PersonBalance {
        private UUID userId;
        private String userName;
        private long balanceCents;
    }

    /**
     * Executes Greedy Graph Debt Simplification Algorithm.
     * Reduces O(N^2) pairwise debts to O(N) minimal transactions (at most N-1).
     */
    public List<SimplifiedDebtResponse> simplifyDebts(Map<UUID, Long> netBalances, Map<UUID, String> userNames) {
        if (netBalances == null || netBalances.isEmpty()) {
            return Collections.emptyList();
        }

        // Net Balance Conservation Invariant Assertion: sum(Net_i) == 0
        long totalSumCents = netBalances.values().stream().mapToLong(Long::longValue).sum();
        if (Math.abs(totalSumCents) > 10) { // allow max 10 paise floating rounding tolerance
            log.warn("Net balance conservation warning: sum is {} cents", totalSumCents);
        }

        List<PersonBalance> creditors = new ArrayList<>();
        List<PersonBalance> debtors = new ArrayList<>();

        for (Map.Entry<UUID, Long> entry : netBalances.entrySet()) {
            UUID userId = entry.getKey();
            long balance = entry.getValue();
            String name = userNames.getOrDefault(userId, "User " + userId.toString().substring(0, 4));

            if (balance > 0) {
                creditors.add(new PersonBalance(userId, name, balance));
            } else if (balance < 0) {
                debtors.add(new PersonBalance(userId, name, Math.abs(balance)));
            }
        }

        // Sort Creditors descending by credit amount, Debtors descending by debt amount
        creditors.sort((a, b) -> Long.compare(b.getBalanceCents(), a.getBalanceCents()));
        debtors.sort((a, b) -> Long.compare(b.getBalanceCents(), a.getBalanceCents()));

        List<SimplifiedDebtResponse> result = new ArrayList<>();
        int cIdx = 0;
        int dIdx = 0;

        while (cIdx < creditors.size() && dIdx < debtors.size()) {
            PersonBalance creditor = creditors.get(cIdx);
            PersonBalance debtor = debtors.get(dIdx);

            long settledAmount = Math.min(creditor.getBalanceCents(), debtor.getBalanceCents());

            if (settledAmount > 0) {
                result.add(SimplifiedDebtResponse.builder()
                        .fromUserId(debtor.getUserId())
                        .fromUserName(debtor.getUserName())
                        .toUserId(creditor.getUserId())
                        .toUserName(creditor.getUserName())
                        .amountCents(settledAmount)
                        .currency("INR")
                        .build());
            }

            creditor.setBalanceCents(creditor.getBalanceCents() - settledAmount);
            debtor.setBalanceCents(debtor.getBalanceCents() - settledAmount);

            if (creditor.getBalanceCents() == 0) cIdx++;
            if (debtor.getBalanceCents() == 0) dIdx++;
        }

        log.info("Debt Simplification reduced group balances to {} minimal transactions", result.size());
        return result;
    }

    public DebtGraphComparisonResponse computeComparison(List<RawDebtResponse> rawDebts, Map<UUID, String> userNames) {
        int rawCount = rawDebts.size();

        // Compute net balance map from raw debts
        Map<UUID, Long> netMap = new HashMap<>();
        for (RawDebtResponse raw : rawDebts) {
            netMap.put(raw.getToUserId(), netMap.getOrDefault(raw.getToUserId(), 0L) + raw.getAmountCents());
            netMap.put(raw.getFromUserId(), netMap.getOrDefault(raw.getFromUserId(), 0L) - raw.getAmountCents());
        }

        List<SimplifiedDebtResponse> simplified = simplifyDebts(netMap, userNames);
        int simplifiedCount = simplified.size();

        double reduction = rawCount > 0
                ? ((double) (rawCount - simplifiedCount) / rawCount) * 100.0
                : 0.0;

        return DebtGraphComparisonResponse.builder()
                .rawTransactionCount(rawCount)
                .simplifiedTransactionCount(simplifiedCount)
                .reductionPercentage(Math.max(0.0, Math.round(reduction * 100.0) / 100.0))
                .rawTransactions(rawDebts)
                .simplifiedTransactions(simplified)
                .build();
    }
}
