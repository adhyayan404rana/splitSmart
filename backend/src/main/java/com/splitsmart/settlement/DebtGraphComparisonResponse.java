package com.splitsmart.settlement;

import java.util.List;

/**
 * Read DTO returned by {@code GET /groups/{id}/debts/comparison}.
 *
 * <p>Wraps the raw and simplified debt graphs side-by-side so the frontend
 * {@code SettlementScreen} can render a before/after visualisation showing
 * how many transactions the simplification eliminated.
 */
public class DebtGraphComparisonResponse {

    private final String                    groupId;
    private final List<RawDebtResponse>        rawDebts;
    private final List<SimplifiedDebtResponse> simplifiedDebts;

    /** Number of transactions in the raw graph. */
    private final int rawTransactionCount;

    /** Number of transactions after simplification. */
    private final int simplifiedTransactionCount;

    /** Percentage reduction in transaction count (0–100). */
    private final int reductionPercent;

    /** Algorithm used: "DP_BITMASK" for exact solver, "GREEDY_HEAP" for fallback. */
    private final String algorithmUsed;

    /** Whether the simplified result is provably optimal. */
    private final boolean isOptimal;

    private DebtGraphComparisonResponse(Builder b) {
        this.groupId                    = b.groupId;
        this.rawDebts                   = b.rawDebts;
        this.simplifiedDebts            = b.simplifiedDebts;
        this.rawTransactionCount        = b.rawDebts != null ? b.rawDebts.size() : 0;
        this.simplifiedTransactionCount = b.simplifiedDebts != null ? b.simplifiedDebts.size() : 0;
        this.algorithmUsed              = b.algorithmUsed;
        this.isOptimal                  = b.isOptimal;
        this.reductionPercent           = rawTransactionCount > 0
                ? (int) (((rawTransactionCount - simplifiedTransactionCount) * 100.0) / rawTransactionCount)
                : 0;
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public String                       getGroupId()                    { return groupId; }
    public List<RawDebtResponse>        getRawDebts()                   { return rawDebts; }
    public List<SimplifiedDebtResponse> getSimplifiedDebts()            { return simplifiedDebts; }
    public int                          getRawTransactionCount()         { return rawTransactionCount; }
    public int                          getSimplifiedTransactionCount()  { return simplifiedTransactionCount; }
    public int                          getReductionPercent()            { return reductionPercent; }
    public String                       getAlgorithmUsed()               { return algorithmUsed; }
    public boolean                      isOptimal()                      { return isOptimal; }

    // ─── Builder ─────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String groupId;
        private List<RawDebtResponse> rawDebts;
        private List<SimplifiedDebtResponse> simplifiedDebts;
        private String algorithmUsed;
        private boolean isOptimal;

        private Builder() {}

        public Builder groupId(String v)                        { groupId = v;         return this; }
        public Builder rawDebts(List<RawDebtResponse> v)        { rawDebts = v;        return this; }
        public Builder simplifiedDebts(List<SimplifiedDebtResponse> v){ simplifiedDebts = v; return this; }
        public Builder algorithmUsed(String v)                  { algorithmUsed = v;   return this; }
        public Builder isOptimal(boolean v)                     { isOptimal = v;       return this; }

        public DebtGraphComparisonResponse build()              { return new DebtGraphComparisonResponse(this); }
    }
}
