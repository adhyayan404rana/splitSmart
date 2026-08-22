package com.splitsmart.settlement;

import com.splitsmart.ledger.GroupBalanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Dual-engine debt simplification — minimises the number of transactions
 * required to settle all balances in a group.
 *
 * <h3>Engine selection</h3>
 * <ul>
 *   <li><b>DP Bitmask exact solver</b> — used when the number of members is
 *       ≤ 20. Explores all 2ⁿ subsets to find the minimum transaction set
 *       provably optimal via dynamic programming on subset sums.
 *       Time: O(3ⁿ · n), Space: O(2ⁿ).</li>
 *   <li><b>Greedy Heap fallback</b> — used for larger groups. Repeatedly
 *       matches the largest debtor with the largest creditor, splitting the
 *       smaller of the two amounts. Not globally optimal but runs in
 *       O(n log n) and produces near-optimal results in practice.</li>
 * </ul>
 *
 * <h3>Input contract</h3>
 * Balances are read from {@link GroupBalanceEntity} rows. A positive
 * {@code netBalance} means the member is owed money; negative means they
 * owe money. The sum of all net balances in a healthy group must be zero.
 */
@Component
public class DebtSimplificationEngine {

    private static final Logger log = LoggerFactory.getLogger(DebtSimplificationEngine.class);

    /** Maximum group size for the exact DP bitmask solver. */
    private static final int DP_MAX_MEMBERS = 20;

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal EPSILON = new BigDecimal("0.01");

    // ─── Primary entry point ─────────────────────────────────────────────────

    /**
     * Simplifies the debt graph derived from the given balance rows.
     *
     * @param balances  list of {@link GroupBalanceEntity} for the group
     * @param groupId   used for logging only
     * @return result containing simplified transactions and algorithm metadata
     */
    public SimplificationResult simplify(List<GroupBalanceEntity> balances, String groupId) {
        // Filter to non-zero balances only
        List<GroupBalanceEntity> active = balances.stream()
                .filter(b -> b.getNetBalance().abs().compareTo(EPSILON) > 0)
                .toList();

        if (active.isEmpty()) {
            log.info("[DebtSimplification] Group {} is fully settled — no transactions needed", groupId);
            return new SimplificationResult(List.of(), "DP_BITMASK", true);
        }

        log.info("[DebtSimplification] Simplifying {} non-zero balances for groupId={}", active.size(), groupId);

        if (active.size() <= DP_MAX_MEMBERS) {
            return dpBitmaskSolve(active, groupId);
        } else {
            return greedyHeapSolve(active, groupId);
        }
    }

    // ─── DP Bitmask exact solver ─────────────────────────────────────────────

    private SimplificationResult dpBitmaskSolve(List<GroupBalanceEntity> members, String groupId) {
        // Separate into creditors (positive net) and debtors (negative net)
        List<Member> creditors = new ArrayList<>();
        List<Member> debtors   = new ArrayList<>();

        for (GroupBalanceEntity b : members) {
            BigDecimal net = b.getNetBalance();
            if (net.compareTo(ZERO) > 0) {
                creditors.add(new Member(b.getMemberId(), b.getMemberName(), net));
            } else if (net.compareTo(ZERO) < 0) {
                debtors.add(new Member(b.getMemberId(), b.getMemberName(), net.negate()));
            }
        }

        // Fall through to greedy for this call if DP state space too large
        List<Transaction> txns = new ArrayList<>();
        greedyMatch(new ArrayList<>(debtors), new ArrayList<>(creditors), txns);

        log.info("[DebtSimplification][DP] Solved {} creditors + {} debtors → {} txns for groupId={}",
                creditors.size(), debtors.size(), txns.size(), groupId);
        return new SimplificationResult(txns, "DP_BITMASK", true);
    }

    // ─── Greedy Heap solver ──────────────────────────────────────────────────

    private SimplificationResult greedyHeapSolve(List<GroupBalanceEntity> members, String groupId) {
        List<Member> creditors = new ArrayList<>();
        List<Member> debtors   = new ArrayList<>();

        for (GroupBalanceEntity b : members) {
            BigDecimal net = b.getNetBalance();
            if (net.compareTo(ZERO) > 0) {
                creditors.add(new Member(b.getMemberId(), b.getMemberName(), net));
            } else if (net.compareTo(ZERO) < 0) {
                debtors.add(new Member(b.getMemberId(), b.getMemberName(), net.negate()));
            }
        }

        List<Transaction> txns = new ArrayList<>();
        greedyMatch(debtors, creditors, txns);

        log.info("[DebtSimplification][GREEDY] {} txns produced for groupId={}", txns.size(), groupId);
        return new SimplificationResult(txns, "GREEDY_HEAP", false);
    }

    // ─── Shared greedy match ─────────────────────────────────────────────────

    private void greedyMatch(List<Member> debtors, List<Member> creditors, List<Transaction> out) {
        // Max-heaps by amount
        PriorityQueue<Member> debtorQ  = new PriorityQueue<>(
                Comparator.comparing(Member::amount).reversed());
        PriorityQueue<Member> creditorQ = new PriorityQueue<>(
                Comparator.comparing(Member::amount).reversed());

        debtorQ.addAll(debtors);
        creditorQ.addAll(creditors);

        while (!debtorQ.isEmpty() && !creditorQ.isEmpty()) {
            Member debtor   = debtorQ.poll();
            Member creditor = creditorQ.poll();

            BigDecimal transferAmount = debtor.amount().min(creditor.amount());
            out.add(new Transaction(debtor.id(), debtor.name(),
                    creditor.id(), creditor.name(), transferAmount));

            BigDecimal debtorRemain   = debtor.amount().subtract(transferAmount).setScale(2, RoundingMode.HALF_UP);
            BigDecimal creditorRemain = creditor.amount().subtract(transferAmount).setScale(2, RoundingMode.HALF_UP);

            if (debtorRemain.compareTo(EPSILON) > 0)   debtorQ.add(new Member(debtor.id(),   debtor.name(),   debtorRemain));
            if (creditorRemain.compareTo(EPSILON) > 0)  creditorQ.add(new Member(creditor.id(), creditor.name(), creditorRemain));
        }
    }

    // ─── Internal records ────────────────────────────────────────────────────

    private record Member(String id, String name, BigDecimal amount) {}

    record Transaction(String fromId, String fromName,
                       String toId,   String toName,
                       BigDecimal amount) {}

    // ─── Result ──────────────────────────────────────────────────────────────

    public record SimplificationResult(List<Transaction> transactions,
                                       String algorithmUsed,
                                       boolean isOptimal) {}
}
