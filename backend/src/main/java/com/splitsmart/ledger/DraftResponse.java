package com.splitsmart.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Read DTO returned by the ledger API for a single expense draft.
 *
 * <p>Consumed by the frontend {@code ConsensusScreen} and {@code AuditScreen}
 * to render draft cards with approval status, member breakdown, and dispute info.
 */
public class DraftResponse {

    private final String        id;
    private final String        groupId;
    private final String        description;
    private final BigDecimal    amount;
    private final String        currency;
    private final String        payerIdentifier;
    private final String        splitType;
    private final String        category;
    private final LocalDate     transactionDate;
    private final List<String>  participants;
    private final String        status;
    private final int           approvalCount;
    private final int           requiredApprovals;
    private final List<String>  approvedBy;
    private final String        disputedBy;
    private final String        disputeReason;
    private final String        extractionSource;
    private final int           extractionTier;
    private final int           confidence;
    private final Instant       createdAt;
    private final Instant       updatedAt;
    private final Instant       expiresAt;

    private DraftResponse(Builder b) {
        this.id               = b.id;
        this.groupId          = b.groupId;
        this.description      = b.description;
        this.amount           = b.amount;
        this.currency         = b.currency;
        this.payerIdentifier  = b.payerIdentifier;
        this.splitType        = b.splitType;
        this.category         = b.category;
        this.transactionDate  = b.transactionDate;
        this.participants     = b.participants;
        this.status           = b.status;
        this.approvalCount    = b.approvalCount;
        this.requiredApprovals = b.requiredApprovals;
        this.approvedBy       = b.approvedBy;
        this.disputedBy       = b.disputedBy;
        this.disputeReason    = b.disputeReason;
        this.extractionSource = b.extractionSource;
        this.extractionTier   = b.extractionTier;
        this.confidence       = b.confidence;
        this.createdAt        = b.createdAt;
        this.updatedAt        = b.updatedAt;
        this.expiresAt        = b.expiresAt;
    }

    // ─── Static factory from entity ──────────────────────────────────────────

    public static DraftResponse from(DraftEntity e) {
        List<String> participants = e.getParticipants() != null && !e.getParticipants().isBlank()
                ? List.of(e.getParticipants().split(","))
                : List.of();
        List<String> approvedBy = e.getApprovedBy() != null && !e.getApprovedBy().isBlank()
                ? List.of(e.getApprovedBy().split(","))
                : List.of();

        return new Builder()
                .id(e.getId())
                .groupId(e.getGroupId())
                .description(e.getDescription())
                .amount(BigDecimal.valueOf(e.getAmountMinor(), 2))
                .currency(e.getCurrency())
                .payerIdentifier(e.getPayerIdentifier())
                .splitType(e.getSplitType())
                .category(e.getCategory())
                .transactionDate(e.getTransactionDate())
                .participants(participants)
                .status(e.getStatus() != null ? e.getStatus().name() : "PENDING")
                .approvalCount(e.getApprovalCount())
                .requiredApprovals(e.getRequiredApprovals())
                .approvedBy(approvedBy)
                .disputedBy(e.getDisputedBy())
                .disputeReason(e.getDisputeReason())
                .extractionSource(e.getExtractionSource())
                .extractionTier(e.getExtractionTier())
                .confidence(e.getConfidence())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .expiresAt(e.getExpiresAt())
                .build();
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public String       getId()               { return id; }
    public String       getGroupId()          { return groupId; }
    public String       getDescription()      { return description; }
    public BigDecimal   getAmount()           { return amount; }
    public String       getCurrency()         { return currency; }
    public String       getPayerIdentifier()  { return payerIdentifier; }
    public String       getSplitType()        { return splitType; }
    public String       getCategory()         { return category; }
    public LocalDate    getTransactionDate()  { return transactionDate; }
    public List<String> getParticipants()     { return participants; }
    public String       getStatus()           { return status; }
    public int          getApprovalCount()    { return approvalCount; }
    public int          getRequiredApprovals(){ return requiredApprovals; }
    public List<String> getApprovedBy()       { return approvedBy; }
    public String       getDisputedBy()       { return disputedBy; }
    public String       getDisputeReason()    { return disputeReason; }
    public String       getExtractionSource() { return extractionSource; }
    public int          getExtractionTier()   { return extractionTier; }
    public int          getConfidence()       { return confidence; }
    public Instant      getCreatedAt()        { return createdAt; }
    public Instant      getUpdatedAt()        { return updatedAt; }
    public Instant      getExpiresAt()        { return expiresAt; }

    // ─── Builder ─────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String id; private String groupId; private String description;
        private BigDecimal amount; private String currency; private String payerIdentifier;
        private String splitType; private String category; private LocalDate transactionDate;
        private List<String> participants; private String status;
        private int approvalCount; private int requiredApprovals;
        private List<String> approvedBy; private String disputedBy; private String disputeReason;
        private String extractionSource; private int extractionTier; private int confidence;
        private Instant createdAt; private Instant updatedAt; private Instant expiresAt;

        private Builder() {}

        public Builder id(String v)               { id = v;               return this; }
        public Builder groupId(String v)           { groupId = v;          return this; }
        public Builder description(String v)       { description = v;      return this; }
        public Builder amount(BigDecimal v)        { amount = v;           return this; }
        public Builder currency(String v)          { currency = v;         return this; }
        public Builder payerIdentifier(String v)   { payerIdentifier = v;  return this; }
        public Builder splitType(String v)         { splitType = v;        return this; }
        public Builder category(String v)          { category = v;         return this; }
        public Builder transactionDate(LocalDate v){ transactionDate = v;  return this; }
        public Builder participants(List<String> v){ participants = v;     return this; }
        public Builder status(String v)            { status = v;           return this; }
        public Builder approvalCount(int v)        { approvalCount = v;    return this; }
        public Builder requiredApprovals(int v)    { requiredApprovals = v;return this; }
        public Builder approvedBy(List<String> v)  { approvedBy = v;       return this; }
        public Builder disputedBy(String v)        { disputedBy = v;       return this; }
        public Builder disputeReason(String v)     { disputeReason = v;    return this; }
        public Builder extractionSource(String v)  { extractionSource = v; return this; }
        public Builder extractionTier(int v)       { extractionTier = v;   return this; }
        public Builder confidence(int v)           { confidence = v;       return this; }
        public Builder createdAt(Instant v)        { createdAt = v;        return this; }
        public Builder updatedAt(Instant v)        { updatedAt = v;        return this; }
        public Builder expiresAt(Instant v)        { expiresAt = v;        return this; }

        public DraftResponse build()               { return new DraftResponse(this); }
    }
}
