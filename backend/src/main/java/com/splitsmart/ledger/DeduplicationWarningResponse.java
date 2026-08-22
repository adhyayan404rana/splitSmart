package com.splitsmart.ledger;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read DTO emitted when the deduplication engine detects a likely duplicate
 * expense draft submission.
 *
 * <p>Returned alongside a {@code 200 OK} (not an error) so the client can
 * surface a non-blocking warning to the user before they confirm re-submission.
 * The original draft is included for side-by-side comparison.
 */
public class DeduplicationWarningResponse {

    /** SHA-256 fingerprint of the incoming expense that triggered the warning. */
    private final String      incomingFingerprint;

    /** Correlation ID of the incoming webhook that was deduplicated. */
    private final String      incomingCorrelationId;

    /** ID of the draft that already exists with a matching fingerprint. */
    private final String      existingDraftId;

    /** Description of the existing draft for UI display. */
    private final String      existingDescription;

    /** Amount of the existing draft in major units. */
    private final BigDecimal  existingAmount;

    /** Currency of the existing draft. */
    private final String      existingCurrency;

    /** When the existing draft was first created. */
    private final Instant     existingCreatedAt;

    /**
     * Similarity score between the incoming text and the existing draft
     * description (0.0–1.0). Values above 0.85 are treated as duplicates.
     */
    private final double      similarityScore;

    /**
     * When the deduplication TTL window for the existing fingerprint expires.
     * After this time, a new submission with the same fingerprint will not
     * trigger a warning.
     */
    private final Instant     ttlExpiresAt;

    private DeduplicationWarningResponse(Builder b) {
        this.incomingFingerprint    = b.incomingFingerprint;
        this.incomingCorrelationId  = b.incomingCorrelationId;
        this.existingDraftId        = b.existingDraftId;
        this.existingDescription    = b.existingDescription;
        this.existingAmount         = b.existingAmount;
        this.existingCurrency       = b.existingCurrency;
        this.existingCreatedAt      = b.existingCreatedAt;
        this.similarityScore        = b.similarityScore;
        this.ttlExpiresAt           = b.ttlExpiresAt;
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public String     getIncomingFingerprint()   { return incomingFingerprint; }
    public String     getIncomingCorrelationId() { return incomingCorrelationId; }
    public String     getExistingDraftId()       { return existingDraftId; }
    public String     getExistingDescription()   { return existingDescription; }
    public BigDecimal getExistingAmount()        { return existingAmount; }
    public String     getExistingCurrency()      { return existingCurrency; }
    public Instant    getExistingCreatedAt()     { return existingCreatedAt; }
    public double     getSimilarityScore()       { return similarityScore; }
    public Instant    getTtlExpiresAt()          { return ttlExpiresAt; }

    // ─── Builder ─────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String incomingFingerprint; private String incomingCorrelationId;
        private String existingDraftId; private String existingDescription;
        private BigDecimal existingAmount; private String existingCurrency;
        private Instant existingCreatedAt; private double similarityScore;
        private Instant ttlExpiresAt;

        private Builder() {}

        public Builder incomingFingerprint(String v)   { incomingFingerprint = v;   return this; }
        public Builder incomingCorrelationId(String v) { incomingCorrelationId = v; return this; }
        public Builder existingDraftId(String v)       { existingDraftId = v;       return this; }
        public Builder existingDescription(String v)   { existingDescription = v;   return this; }
        public Builder existingAmount(BigDecimal v)    { existingAmount = v;        return this; }
        public Builder existingCurrency(String v)      { existingCurrency = v;      return this; }
        public Builder existingCreatedAt(Instant v)    { existingCreatedAt = v;     return this; }
        public Builder similarityScore(double v)       { similarityScore = v;       return this; }
        public Builder ttlExpiresAt(Instant v)         { ttlExpiresAt = v;          return this; }

        public DeduplicationWarningResponse build()    { return new DeduplicationWarningResponse(this); }
    }
}
