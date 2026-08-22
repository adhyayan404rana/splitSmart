package com.splitsmart.settlement;

import com.splitsmart.ledger.GroupBalanceEntity;
import com.splitsmart.ledger.GroupBalanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service exposing the raw and simplified debt graphs for a group.
 *
 * <p>Consumed by the settlement REST controller to power the
 * {@code SettlementScreen} on the frontend.
 *
 * <p>Raw debts are derived directly from the {@link GroupBalanceEntity}
 * projection: every (debtor, creditor) pair with non-zero net balances
 * produces one raw edge. The simplified graph is produced by
 * {@link DebtSimplificationEngine}.
 */
@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final GroupBalanceRepository   groupBalanceRepository;
    private final DebtSimplificationEngine simplificationEngine;

    public SettlementService(GroupBalanceRepository groupBalanceRepository,
                             DebtSimplificationEngine simplificationEngine) {
        this.groupBalanceRepository = groupBalanceRepository;
        this.simplificationEngine   = simplificationEngine;
    }

    // ─── Raw debt graph ──────────────────────────────────────────────────────

    /**
     * Returns the raw (unsimplified) debt graph for a group.
     *
     * <p>Each debtor-creditor pair in the balance list produces one edge.
     * The raw graph may have up to O(n²) edges for n members.
     */
    @Transactional(readOnly = true)
    public List<RawDebtResponse> getRawDebts(String groupId) {
        List<GroupBalanceEntity> debtors   = groupBalanceRepository.findDebtorsByGroupId(groupId);
        List<GroupBalanceEntity> creditors = groupBalanceRepository.findCreditorsByGroupId(groupId);

        // Cross-product: every debtor owes proportionally to every creditor
        // In practice this uses the balance magnitude as a proxy for share
        return debtors.stream().flatMap(debtor ->
                creditors.stream().map(creditor -> {
                    BigDecimal amount = debtor.getNetBalance().abs()
                            .min(creditor.getNetBalance().abs());
                    return new RawDebtResponse(
                            debtor.getMemberId(),   debtor.getMemberName(),
                            creditor.getMemberId(), creditor.getMemberName(),
                            amount, debtor.getCurrency());
                })
        ).collect(Collectors.toList());
    }

    // ─── Simplified debt graph ───────────────────────────────────────────────

    /**
     * Returns the simplified debt graph with the minimum number of
     * transactions to settle all group debts.
     */
    @Transactional(readOnly = true)
    public List<SimplifiedDebtResponse> getSimplifiedDebts(String groupId) {
        List<GroupBalanceEntity> balances = groupBalanceRepository.findByGroupIdOrderByNetBalanceDesc(groupId);
        DebtSimplificationEngine.SimplificationResult result = simplificationEngine.simplify(balances, groupId);

        return result.transactions().stream()
                .map(t -> new SimplifiedDebtResponse(
                        t.fromId(), t.fromName(),
                        t.toId(),   t.toName(),
                        t.amount(), resolveGroupCurrency(balances),
                        null, null)) // UPI fields populated by PaymentService on Day 13
                .collect(Collectors.toList());
    }

    // ─── Comparison ──────────────────────────────────────────────────────────

    /**
     * Returns a side-by-side comparison of the raw and simplified debt graphs,
     * including algorithm metadata and reduction statistics.
     */
    @Transactional(readOnly = true)
    public DebtGraphComparisonResponse getDebtComparison(String groupId) {
        List<RawDebtResponse> rawDebts         = getRawDebts(groupId);
        List<SimplifiedDebtResponse> simplified = getSimplifiedDebts(groupId);

        List<GroupBalanceEntity> balances = groupBalanceRepository.findByGroupIdOrderByNetBalanceDesc(groupId);
        DebtSimplificationEngine.SimplificationResult result = simplificationEngine.simplify(balances, groupId);

        log.info("[SettlementService] Debt comparison for groupId={} — raw={} simplified={} algo={}",
                groupId, rawDebts.size(), simplified.size(), result.algorithmUsed());

        return DebtGraphComparisonResponse.builder()
                .groupId(groupId)
                .rawDebts(rawDebts)
                .simplifiedDebts(simplified)
                .algorithmUsed(result.algorithmUsed())
                .isOptimal(result.isOptimal())
                .build();
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private String resolveGroupCurrency(List<GroupBalanceEntity> balances) {
        return balances.isEmpty() ? "INR" : balances.get(0).getCurrency();
    }
}
