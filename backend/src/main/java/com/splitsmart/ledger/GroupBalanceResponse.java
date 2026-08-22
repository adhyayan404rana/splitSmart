package com.splitsmart.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Read DTO summarising the financial balance of all members in a group.
 *
 * <p>Returned by {@code GET /groups/{id}/balances}. Consumed by the
 * frontend {@code OverviewScreen} to render the balance dashboard,
 * per-member breakdown, and total group spend.
 */
public class GroupBalanceResponse {

    private final String              groupId;
    private final String              currency;
    private final BigDecimal          totalSpend;
    private final List<MemberBalance> members;
    private final boolean             fullySettled;
    private final Instant             asOf;

    private GroupBalanceResponse(Builder b) {
        this.groupId      = b.groupId;
        this.currency     = b.currency;
        this.totalSpend   = b.totalSpend;
        this.members      = b.members;
        this.fullySettled = b.fullySettled;
        this.asOf         = b.asOf != null ? b.asOf : Instant.now();
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public String              getGroupId()      { return groupId; }
    public String              getCurrency()     { return currency; }
    public BigDecimal          getTotalSpend()   { return totalSpend; }
    public List<MemberBalance> getMembers()      { return members; }
    public boolean             isFullySettled()  { return fullySettled; }
    public Instant             getAsOf()         { return asOf; }

    // ─── Nested member balance ────────────────────────────────────────────────

    /**
     * Balance summary for a single group member.
     *
     * <p>{@code netBalance} semantics:
     * <ul>
     *   <li>Positive → member is owed money (paid more than their share)</li>
     *   <li>Negative → member owes money (share exceeds what they paid)</li>
     * </ul>
     */
    public static class MemberBalance {

        private final String     memberId;
        private final String     memberName;
        private final BigDecimal totalPaid;
        private final BigDecimal totalOwed;
        private final BigDecimal netBalance;
        private final int        draftCount;

        public MemberBalance(String memberId, String memberName,
                             BigDecimal totalPaid, BigDecimal totalOwed,
                             BigDecimal netBalance, int draftCount) {
            this.memberId   = memberId;
            this.memberName = memberName;
            this.totalPaid  = totalPaid;
            this.totalOwed  = totalOwed;
            this.netBalance = netBalance;
            this.draftCount = draftCount;
        }

        // Static factory from entity
        public static MemberBalance from(GroupBalanceEntity e) {
            return new MemberBalance(
                    e.getMemberId(), e.getMemberName(),
                    e.getTotalPaid(), e.getTotalOwed(),
                    e.getNetBalance(), e.getDraftCount());
        }

        public String     getMemberId()   { return memberId; }
        public String     getMemberName() { return memberName; }
        public BigDecimal getTotalPaid()  { return totalPaid; }
        public BigDecimal getTotalOwed()  { return totalOwed; }
        public BigDecimal getNetBalance() { return netBalance; }
        public int        getDraftCount() { return draftCount; }
    }

    // ─── Builder ─────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String groupId; private String currency;
        private BigDecimal totalSpend; private List<MemberBalance> members;
        private boolean fullySettled; private Instant asOf;

        private Builder() {}

        public Builder groupId(String v)              { groupId = v;      return this; }
        public Builder currency(String v)             { currency = v;     return this; }
        public Builder totalSpend(BigDecimal v)       { totalSpend = v;   return this; }
        public Builder members(List<MemberBalance> v) { members = v;      return this; }
        public Builder fullySettled(boolean v)        { fullySettled = v; return this; }
        public Builder asOf(Instant v)                { asOf = v;         return this; }

        public GroupBalanceResponse build()           { return new GroupBalanceResponse(this); }
    }
}
