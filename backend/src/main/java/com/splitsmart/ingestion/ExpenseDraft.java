package com.splitsmart.ingestion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Immutable value object representing a structured expense draft produced
 * by the 3-tier NLP pipeline.
 *
 * <p>All monetary amounts are stored in <em>minor units</em> (integer paise /
 * cents) to avoid floating-point precision issues throughout the pipeline.
 *
 * <p>Instances are produced by {@link IngestionService} after the pipeline
 * resolves to a confidence threshold above {@code MINIMUM_CONFIDENCE}.
 * They are then persisted as {@code DraftEntity} records awaiting group
 * consensus approval.
 */
public final class ExpenseDraft {

    /** Minimum confidence score (0–100) required to auto-create a draft. */
    public static final int MINIMUM_CONFIDENCE = 60;

    // ─── Identity & provenance ───────────────────────────────────────────────

    /** Correlation ID from the inbound webhook envelope. */
    private final String correlationId;

    /** SplitSmart group ID this draft belongs to. */
    private final String groupId;

    /** Channel through which the raw text arrived. */
    private final ExtractionSource source;

    /** Pipeline tier that produced this draft (1 = FastPath, 2 = NER, 3 = LLM). */
    private final int extractionTier;

    /** Confidence score 0–100 assigned by the winning parser tier. */
    private final int confidence;

    // ─── Expense semantics ──────────────────────────────────────────────────

    /** Human-readable description extracted from the input. */
    private final String description;

    /** Total amount in minor units (e.g. 400_000 for ₹4,000.00). */
    private final long totalMinorUnits;

    /** ISO 4217 currency code, e.g. "INR". */
    private final String currencyCode;

    /**
     * Name or identifier of the person who paid.
     * May be a phone number, Telegram username, or display name
     * depending on the ingestion channel.
     */
    private final String payerIdentifier;

    /**
     * Split strategy resolved by the pipeline.
     * Mirrors {@code Draft.split} on the frontend model.
     */
    private final SplitType splitType;

    /**
     * Ordered list of participant identifiers (same format as
     * {@code payerIdentifier}).  Empty list means the group's full
     * member set should be inferred at consensus time.
     */
    private final List<String> participants;

    /**
     * Optional expense category tag for UI display and analytics.
     */
    private final String category;

    /** Wall-clock timestamp when the draft was created by the pipeline. */
    private final Instant createdAt;

    /** Raw text that was fed into the NLP pipeline (for audit trail). */
    private final String rawInput;

    // ─── Constructor ────────────────────────────────────────────────────────

    private ExpenseDraft(Builder builder) {
        this.correlationId   = Objects.requireNonNull(builder.correlationId, "correlationId");
        this.groupId         = Objects.requireNonNull(builder.groupId, "groupId");
        this.source          = Objects.requireNonNull(builder.source, "source");
        this.extractionTier  = builder.extractionTier;
        this.confidence      = builder.confidence;
        this.description     = builder.description != null ? builder.description : "";
        this.totalMinorUnits = builder.totalMinorUnits;
        this.currencyCode    = builder.currencyCode != null ? builder.currencyCode : "INR";
        this.payerIdentifier = builder.payerIdentifier != null ? builder.payerIdentifier : "";
        this.splitType       = builder.splitType != null ? builder.splitType : SplitType.EQUAL;
        this.participants    = builder.participants != null
                                   ? Collections.unmodifiableList(builder.participants)
                                   : Collections.emptyList();
        this.category        = builder.category != null ? builder.category : "Bills";
        this.createdAt       = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.rawInput        = builder.rawInput != null ? builder.rawInput : "";
    }

    // ─── Getters ────────────────────────────────────────────────────────────

    public String getCorrelationId()          { return correlationId; }
    public String getGroupId()                { return groupId; }
    public ExtractionSource getSource()       { return source; }
    public int getExtractionTier()            { return extractionTier; }
    public int getConfidence()                { return confidence; }
    public String getDescription()            { return description; }
    public long getTotalMinorUnits()          { return totalMinorUnits; }
    public String getCurrencyCode()           { return currencyCode; }
    public String getPayerIdentifier()        { return payerIdentifier; }
    public SplitType getSplitType()           { return splitType; }
    public List<String> getParticipants()     { return participants; }
    public String getCategory()               { return category; }
    public Instant getCreatedAt()             { return createdAt; }
    public String getRawInput()               { return rawInput; }

    /**
     * Returns the total amount as a {@link BigDecimal} in major units,
     * e.g. 4000.00 for ₹4,000.
     */
    public BigDecimal getTotalMajorUnits() {
        return BigDecimal.valueOf(totalMinorUnits, 2);
    }

    public boolean meetsMinimumConfidence() {
        return confidence >= MINIMUM_CONFIDENCE;
    }

    // ─── Split type enum ────────────────────────────────────────────────────

    public enum SplitType {
        EQUAL,
        EXACT,
        PERCENTAGE
    }

    // ─── Builder ────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String         correlationId;
        private String         groupId;
        private ExtractionSource source;
        private int            extractionTier;
        private int            confidence;
        private String         description;
        private long           totalMinorUnits;
        private String         currencyCode;
        private String         payerIdentifier;
        private SplitType      splitType;
        private List<String>   participants;
        private String         category;
        private Instant        createdAt;
        private String         rawInput;

        private Builder() {}

        public Builder correlationId(String v)         { correlationId = v;   return this; }
        public Builder groupId(String v)               { groupId = v;         return this; }
        public Builder source(ExtractionSource v)      { source = v;          return this; }
        public Builder extractionTier(int v)           { extractionTier = v;  return this; }
        public Builder confidence(int v)               { confidence = v;      return this; }
        public Builder description(String v)           { description = v;     return this; }
        public Builder totalMinorUnits(long v)         { totalMinorUnits = v; return this; }
        public Builder currencyCode(String v)          { currencyCode = v;    return this; }
        public Builder payerIdentifier(String v)       { payerIdentifier = v; return this; }
        public Builder splitType(SplitType v)          { splitType = v;       return this; }
        public Builder participants(List<String> v)    { participants = v;    return this; }
        public Builder category(String v)              { category = v;        return this; }
        public Builder createdAt(Instant v)            { createdAt = v;       return this; }
        public Builder rawInput(String v)              { rawInput = v;        return this; }

        public ExpenseDraft build() { return new ExpenseDraft(this); }
    }

    @Override
    public String toString() {
        return "ExpenseDraft{" +
               "correlationId='" + correlationId + '\'' +
               ", groupId='" + groupId + '\'' +
               ", source=" + source +
               ", tier=" + extractionTier +
               ", confidence=" + confidence +
               ", description='" + description + '\'' +
               ", total=" + getTotalMajorUnits() + " " + currencyCode +
               ", split=" + splitType +
               ", participants=" + participants.size() +
               '}';
    }
}
