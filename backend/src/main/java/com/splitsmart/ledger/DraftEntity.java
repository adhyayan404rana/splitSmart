package com.splitsmart.ledger;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity representing a consensus-pending expense draft.
 *
 * <p>A {@code DraftEntity} is the mutable, short-lived counterpart to the
 * immutable {@link EventEntity}. It holds an expense proposal in progress
 * until the required approval quorum is met, after which a
 * {@code DraftApproved} event is appended to the event store and the draft
 * is marked {@link Status#FINALIZED}.
 *
 * <h3>Lifecycle</h3>
 * <pre>
 *   PENDING ──► APPROVED  (quorum reached)
 *           ──► DISPUTED  (any member raises a dispute)
 *           ──► EXPIRED   (TTL elapsed without quorum)
 * </pre>
 *
 * <p>Mapped to table {@code drafts} (created by Flyway V3 migration).
 */
@Entity
@Table(
    name = "drafts",
    indexes = {
        @Index(name = "idx_drafts_group_status", columnList = "group_id, status"),
        @Index(name = "idx_drafts_group_created", columnList = "group_id, created_at")
    }
)
public class DraftEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    /** Group this draft belongs to. */
    @Column(name = "group_id", nullable = false, updatable = false)
    private String groupId;

    /** ID of the event that originally created this draft. */
    @Column(name = "origin_event_id", updatable = false)
    private String originEventId;

    /** Correlation ID from the inbound webhook envelope. */
    @Column(name = "correlation_id", updatable = false, length = 64)
    private String correlationId;

    // ─── Expense fields ──────────────────────────────────────────────────────

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    /** Total amount in minor units (paise). */
    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Display name / identifier of the person who paid. */
    @Column(name = "payer_identifier", nullable = false)
    private String payerIdentifier;

    /** Split strategy: EQUAL, EXACT, or PERCENTAGE. */
    @Column(name = "split_type", nullable = false, length = 20)
    private String splitType;

    /** Category tag: Food, Transport, Stay, Bills. */
    @Column(name = "category", length = 40)
    private String category;

    /**
     * Actual date on which the real-world transaction occurred.
     * Distinct from {@code createdAt} (when the draft was created).
     */
    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    /** Comma-separated participant identifiers for denormalised fast reads. */
    @Column(name = "participants", length = 1024)
    private String participants;

    // ─── Consensus tracking ──────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    /** Number of approvals received so far. */
    @Column(name = "approval_count", nullable = false)
    private int approvalCount;

    /** Minimum approvals required to finalize (set at group creation). */
    @Column(name = "required_approvals", nullable = false)
    private int requiredApprovals;

    /** Comma-separated user IDs who have approved. */
    @Column(name = "approved_by", length = 2048)
    private String approvedBy;

    /** User ID who raised a dispute, if any. */
    @Column(name = "disputed_by", length = 64)
    private String disputedBy;

    /** Free-text reason provided when a dispute is raised. */
    @Column(name = "dispute_reason", length = 500)
    private String disputeReason;

    // ─── Provenance ──────────────────────────────────────────────────────────

    /** Channel through which this draft was ingested. */
    @Column(name = "extraction_source", length = 30)
    private String extractionSource;

    /** NLP tier that produced this draft (1=FastPath, 2=NER, 3=LLM). */
    @Column(name = "extraction_tier")
    private int extractionTier;

    /** Confidence score assigned by the NLP tier (0–100). */
    @Column(name = "confidence")
    private int confidence;

    // ─── Timestamps ──────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Wall-clock time when the draft expires if quorum is not reached. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    // ─── Status enum ────────────────────────────────────────────────────────

    public enum Status {
        PENDING,
        APPROVED,
        DISPUTED,
        EXPIRED,
        FINALIZED
    }

    // ─── Constructors ────────────────────────────────────────────────────────

    protected DraftEntity() {}

    public DraftEntity(String groupId) {
        this.id               = UUID.randomUUID().toString();
        this.groupId          = groupId;
        this.status           = Status.PENDING;
        this.approvalCount    = 0;
        this.requiredApprovals = 2;
        this.currency         = "INR";
    }

    // ─── Getters & setters ───────────────────────────────────────────────────

    public String    getId()                { return id; }
    public String    getGroupId()           { return groupId; }
    public String    getOriginEventId()     { return originEventId; }
    public String    getCorrelationId()     { return correlationId; }
    public String    getDescription()       { return description; }
    public long      getAmountMinor()       { return amountMinor; }
    public String    getCurrency()          { return currency; }
    public String    getPayerIdentifier()   { return payerIdentifier; }
    public String    getSplitType()         { return splitType; }
    public String    getCategory()          { return category; }
    public LocalDate getTransactionDate()   { return transactionDate; }
    public String    getParticipants()      { return participants; }
    public Status    getStatus()            { return status; }
    public int       getApprovalCount()     { return approvalCount; }
    public int       getRequiredApprovals() { return requiredApprovals; }
    public String    getApprovedBy()        { return approvedBy; }
    public String    getDisputedBy()        { return disputedBy; }
    public String    getDisputeReason()     { return disputeReason; }
    public String    getExtractionSource()  { return extractionSource; }
    public int       getExtractionTier()    { return extractionTier; }
    public int       getConfidence()        { return confidence; }
    public Instant   getCreatedAt()         { return createdAt; }
    public Instant   getUpdatedAt()         { return updatedAt; }
    public Instant   getExpiresAt()         { return expiresAt; }

    public void setOriginEventId(String v)   { originEventId = v; }
    public void setCorrelationId(String v)   { correlationId = v; }
    public void setDescription(String v)     { description = v; }
    public void setAmountMinor(long v)       { amountMinor = v; }
    public void setCurrency(String v)        { currency = v; }
    public void setPayerIdentifier(String v) { payerIdentifier = v; }
    public void setSplitType(String v)       { splitType = v; }
    public void setCategory(String v)        { category = v; }
    public void setTransactionDate(LocalDate v) { transactionDate = v; }
    public void setParticipants(String v)    { participants = v; }
    public void setStatus(Status v)          { status = v; }
    public void setApprovalCount(int v)      { approvalCount = v; }
    public void setRequiredApprovals(int v)  { requiredApprovals = v; }
    public void setApprovedBy(String v)      { approvedBy = v; }
    public void setDisputedBy(String v)      { disputedBy = v; }
    public void setDisputeReason(String v)   { disputeReason = v; }
    public void setExtractionSource(String v){ extractionSource = v; }
    public void setExtractionTier(int v)     { extractionTier = v; }
    public void setConfidence(int v)         { confidence = v; }
    public void setExpiresAt(Instant v)      { expiresAt = v; }

    @Override
    public String toString() {
        return "DraftEntity{id='" + id + "', groupId='" + groupId +
               "', status=" + status + ", amount=" + amountMinor + " " + currency + "}";
    }
}
