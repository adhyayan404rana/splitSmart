package com.splitsmart.ledger;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity representing a materialized group balance view.
 *
 * <p>This is the CQRS read-side projection for the ledger. Rather than
 * replaying the full event stream on every balance request, the
 * {@code LedgerProjectionWorker} maintains a pre-computed net balance for
 * each (group, member) pair that is updated incrementally as new
 * {@link EventEntity} records arrive.
 *
 * <h3>Balance semantics</h3>
 * <ul>
 *   <li>A <b>positive</b> {@code netBalance} means the member is owed money
 *       (they have paid more than their share).</li>
 *   <li>A <b>negative</b> {@code netBalance} means the member owes money
 *       to the group (their share exceeds what they have paid).</li>
 *   <li>{@code totalPaid} tracks the cumulative amount the member has paid
 *       across all approved drafts.</li>
 *   <li>{@code totalOwed} tracks the cumulative amount the member owes
 *       across all approved drafts.</li>
 * </ul>
 *
 * <p>Mapped to table {@code group_balances} (created by Flyway V3 migration).
 * The ({@code group_id}, {@code member_id}) pair has a unique constraint so
 * the projection worker can use an upsert pattern.
 */
@Entity
@Table(
    name = "group_balances",
    uniqueConstraints = @UniqueConstraint(
            name = "uq_group_balances_group_member",
            columnNames = {"group_id", "member_id"}
    ),
    indexes = {
        @Index(name = "idx_group_balances_group", columnList = "group_id"),
        @Index(name = "idx_group_balances_member", columnList = "member_id")
    }
)
public class GroupBalanceEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "group_id", nullable = false, updatable = false)
    private String groupId;

    @Column(name = "member_id", nullable = false, updatable = false)
    private String memberId;

    /** Display name of the member — denormalized for fast read. */
    @Column(name = "member_name", length = 120)
    private String memberName;

    /** Cumulative amount paid by this member (in major units, e.g. ₹). */
    @Column(name = "total_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPaid = BigDecimal.ZERO;

    /** Cumulative amount owed by this member (in major units). */
    @Column(name = "total_owed", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalOwed = BigDecimal.ZERO;

    /**
     * Net balance = totalPaid - totalOwed.
     * Positive → member is owed; Negative → member owes.
     */
    @Column(name = "net_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal netBalance = BigDecimal.ZERO;

    /** ISO-4217 currency code for this balance entry. */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    /**
     * Last event version that was reflected in this projection row.
     * Used by the projection worker for incremental updates.
     */
    @Column(name = "last_event_version", nullable = false)
    private long lastEventVersion = 0L;

    /** Total number of approved drafts this member participated in. */
    @Column(name = "draft_count", nullable = false)
    private int draftCount = 0;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ─── Constructors ────────────────────────────────────────────────────────

    protected GroupBalanceEntity() {}

    public GroupBalanceEntity(String groupId, String memberId, String memberName, String currency) {
        this.groupId    = groupId;
        this.memberId   = memberId;
        this.memberName = memberName;
        this.currency   = currency;
    }

    // ─── Balance mutation helpers ────────────────────────────────────────────

    /**
     * Records a payment made by this member and updates the net balance.
     *
     * @param amount amount paid in major units (positive)
     */
    public void recordPayment(BigDecimal amount) {
        this.totalPaid  = this.totalPaid.add(amount);
        this.netBalance = this.totalPaid.subtract(this.totalOwed);
    }

    /**
     * Records an expense share owed by this member and updates the net balance.
     *
     * @param amount share amount in major units (positive)
     */
    public void recordOwed(BigDecimal amount) {
        this.totalOwed  = this.totalOwed.add(amount);
        this.netBalance = this.totalPaid.subtract(this.totalOwed);
        this.draftCount++;
    }

    // ─── Getters & setters ───────────────────────────────────────────────────

    public String     getId()               { return id; }
    public String     getGroupId()          { return groupId; }
    public String     getMemberId()         { return memberId; }
    public String     getMemberName()       { return memberName; }
    public BigDecimal getTotalPaid()        { return totalPaid; }
    public BigDecimal getTotalOwed()        { return totalOwed; }
    public BigDecimal getNetBalance()       { return netBalance; }
    public String     getCurrency()         { return currency; }
    public long       getLastEventVersion() { return lastEventVersion; }
    public int        getDraftCount()       { return draftCount; }
    public Instant    getUpdatedAt()        { return updatedAt; }

    public void setMemberName(String v)       { memberName = v; }
    public void setTotalPaid(BigDecimal v)    { totalPaid = v; }
    public void setTotalOwed(BigDecimal v)    { totalOwed = v; }
    public void setNetBalance(BigDecimal v)   { netBalance = v; }
    public void setCurrency(String v)         { currency = v; }
    public void setLastEventVersion(long v)   { lastEventVersion = v; }
    public void setDraftCount(int v)          { draftCount = v; }

    @Override
    public String toString() {
        return "GroupBalanceEntity{groupId='" + groupId + "', memberId='" + memberId +
               "', net=" + netBalance + " " + currency + "}";
    }
}
