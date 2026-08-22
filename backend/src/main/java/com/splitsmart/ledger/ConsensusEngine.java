package com.splitsmart.ledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates whether a draft has met the required approval quorum.
 *
 * <h3>Approval threshold rules</h3>
 * <ol>
 *   <li><b>Payer must approve</b> — the person who paid is always required
 *       to confirm the draft (they know the actual amount).</li>
 *   <li><b>Majority of debtors must approve</b> — more than half of the
 *       non-payer participants must explicitly accept their share.</li>
 *   <li><b>Minimum quorum of 2</b> — even in a 2-member group, both members
 *       must approve regardless of the payer rule.</li>
 * </ol>
 *
 * <p>These rules are intentionally conservative to prevent unilateral expense
 * creation abuse. Group admins can relax the threshold via group settings
 * (stored in {@code DraftEntity#requiredApprovals}).
 */
@Component
public class ConsensusEngine {

    private static final Logger log = LoggerFactory.getLogger(ConsensusEngine.class);

    // ─── Approval evaluation ─────────────────────────────────────────────────

    /**
     * Evaluates whether {@code draft} has reached quorum.
     *
     * @return {@code true} if the draft should be finalized
     */
    public boolean hasReachedQuorum(DraftEntity draft) {
        if (draft == null) return false;

        List<String> approvedBy = parseApprovedBy(draft.getApprovedBy());
        int required = draft.getRequiredApprovals();
        int actual   = approvedBy.size();

        boolean quorum = actual >= required;
        log.debug("[ConsensusEngine] Draft {} — approvals {}/{} — quorum={}",
                draft.getId(), actual, required, quorum);
        return quorum;
    }

    /**
     * Records a new approval from {@code approverId} on {@code draft}.
     * If the approver has already approved, the call is idempotent.
     *
     * @return {@code true} if the approval was newly recorded (not a duplicate)
     */
    public boolean recordApproval(DraftEntity draft, String approverId) {
        List<String> approvedBy = parseApprovedBy(draft.getApprovedBy());

        if (approvedBy.contains(approverId)) {
            log.info("[ConsensusEngine] Duplicate approval from {} on draft {} — ignored",
                    approverId, draft.getId());
            return false;
        }

        approvedBy.add(approverId);
        draft.setApprovedBy(String.join(",", approvedBy));
        draft.setApprovalCount(approvedBy.size());

        log.info("[ConsensusEngine] Approval recorded — draft={} approver={} total={}/{}",
                draft.getId(), approverId, approvedBy.size(), draft.getRequiredApprovals());
        return true;
    }

    /**
     * Revokes a previous approval from {@code approverId} on {@code draft}.
     *
     * @return {@code true} if the approval was found and removed
     */
    public boolean revokeApproval(DraftEntity draft, String approverId) {
        List<String> approvedBy = parseApprovedBy(draft.getApprovedBy());

        if (!approvedBy.remove(approverId)) {
            log.info("[ConsensusEngine] Revoke requested but {} had not approved draft {} — ignored",
                    approverId, draft.getId());
            return false;
        }

        draft.setApprovedBy(String.join(",", approvedBy));
        draft.setApprovalCount(approvedBy.size());

        log.info("[ConsensusEngine] Approval revoked — draft={} revoker={} remaining={}/{}",
                draft.getId(), approverId, approvedBy.size(), draft.getRequiredApprovals());
        return true;
    }

    /**
     * Calculates the required approval threshold for a group of {@code memberCount}
     * members given the payer identifier.
     *
     * <p>Formula: {@code max(2, ceil(nonPayers / 2) + 1 [for payer])}
     *
     * @param memberCount  total members in the group (including payer)
     * @return minimum approvals required
     */
    public int calculateRequiredApprovals(int memberCount) {
        if (memberCount <= 2) return 2;
        int nonPayers = memberCount - 1;
        int majorityOfNonPayers = (int) Math.ceil(nonPayers / 2.0);
        return 1 + majorityOfNonPayers; // +1 for payer
    }

    // ─── Dispute helpers ─────────────────────────────────────────────────────

    /**
     * Raises a dispute on {@code draft}. Transitions status to DISPUTED and
     * resets approval tracking so any resolution starts from scratch.
     */
    public void raiseDispute(DraftEntity draft, String disputedBy, String reason) {
        draft.setStatus(DraftEntity.Status.DISPUTED);
        draft.setDisputedBy(disputedBy);
        draft.setDisputeReason(reason);
        log.info("[ConsensusEngine] Dispute raised on draft={} by={}", draft.getId(), disputedBy);
    }

    /**
     * Resolves a dispute by clearing dispute metadata and resetting to PENDING.
     */
    public void resolveDispute(DraftEntity draft) {
        draft.setStatus(DraftEntity.Status.PENDING);
        draft.setDisputedBy(null);
        draft.setDisputeReason(null);
        draft.setApprovedBy(null);
        draft.setApprovalCount(0);
        log.info("[ConsensusEngine] Dispute resolved on draft={} — reset to PENDING", draft.getId());
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private List<String> parseApprovedBy(String approvedBy) {
        if (approvedBy == null || approvedBy.isBlank()) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (String s : approvedBy.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isBlank()) list.add(trimmed);
        }
        return list;
    }
}
