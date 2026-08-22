package com.splitsmart.ledger;

import java.util.List;

/**
 * Read DTO summarising the current consensus state for a specific draft.
 *
 * <p>Returned by {@code GET /drafts/{id}/consensus}. Polled by the frontend
 * {@code ConsensusScreen} and also pushed via SSE whenever an approval or
 * dispute event occurs.
 */
public class ConsensusStatusResponse {

    private final String       draftId;
    private final String       groupId;
    private final String       status;
    private final int          approvalCount;
    private final int          requiredApprovals;
    private final int          remainingApprovals;
    private final List<String> approvedBy;
    private final List<String> pendingApprovers;
    private final boolean      quorumReached;
    private final boolean      isDisputed;
    private final String       disputedBy;
    private final String       disputeReason;

    /** Percentage of required approvals received (0–100). */
    private final int progressPercent;

    private ConsensusStatusResponse(Builder b) {
        this.draftId            = b.draftId;
        this.groupId            = b.groupId;
        this.status             = b.status;
        this.approvalCount      = b.approvalCount;
        this.requiredApprovals  = b.requiredApprovals;
        this.remainingApprovals = Math.max(0, b.requiredApprovals - b.approvalCount);
        this.approvedBy         = b.approvedBy;
        this.pendingApprovers   = b.pendingApprovers;
        this.quorumReached      = b.approvalCount >= b.requiredApprovals;
        this.isDisputed         = b.isDisputed;
        this.disputedBy         = b.disputedBy;
        this.disputeReason      = b.disputeReason;
        this.progressPercent    = b.requiredApprovals > 0
                ? Math.min(100, (b.approvalCount * 100) / b.requiredApprovals) : 0;
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public String       getDraftId()            { return draftId; }
    public String       getGroupId()            { return groupId; }
    public String       getStatus()             { return status; }
    public int          getApprovalCount()      { return approvalCount; }
    public int          getRequiredApprovals()  { return requiredApprovals; }
    public int          getRemainingApprovals() { return remainingApprovals; }
    public List<String> getApprovedBy()         { return approvedBy; }
    public List<String> getPendingApprovers()   { return pendingApprovers; }
    public boolean      isQuorumReached()       { return quorumReached; }
    public boolean      isDisputed()            { return isDisputed; }
    public String       getDisputedBy()         { return disputedBy; }
    public String       getDisputeReason()      { return disputeReason; }
    public int          getProgressPercent()    { return progressPercent; }

    // ─── Builder ─────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String draftId; private String groupId; private String status;
        private int approvalCount; private int requiredApprovals;
        private List<String> approvedBy; private List<String> pendingApprovers;
        private boolean isDisputed; private String disputedBy; private String disputeReason;

        private Builder() {}

        public Builder draftId(String v)             { draftId = v;           return this; }
        public Builder groupId(String v)             { groupId = v;           return this; }
        public Builder status(String v)              { status = v;            return this; }
        public Builder approvalCount(int v)          { approvalCount = v;     return this; }
        public Builder requiredApprovals(int v)      { requiredApprovals = v; return this; }
        public Builder approvedBy(List<String> v)    { approvedBy = v;        return this; }
        public Builder pendingApprovers(List<String> v){ pendingApprovers = v; return this; }
        public Builder isDisputed(boolean v)         { isDisputed = v;        return this; }
        public Builder disputedBy(String v)          { disputedBy = v;        return this; }
        public Builder disputeReason(String v)       { disputeReason = v;     return this; }

        public ConsensusStatusResponse build()       { return new ConsensusStatusResponse(this); }
    }
}
