package com.splitsmart.settlement;

import java.math.BigDecimal;

/**
 * Read DTO representing a single raw (unsimplified) debt edge in the group's
 * debt graph.
 *
 * <p>A raw debt means: {@code debtor} owes {@code creditor} the given
 * {@code amount} based on the raw balance calculation before any
 * simplification pass.
 *
 * <p>The raw graph may contain many edges (O(n²) in the worst case). The
 * {@link DebtSimplificationEngine} reduces this to a minimal set.
 */
public class RawDebtResponse {

    private final String     debtorId;
    private final String     debtorName;
    private final String     creditorId;
    private final String     creditorName;
    private final BigDecimal amount;
    private final String     currency;

    public RawDebtResponse(String debtorId, String debtorName,
                           String creditorId, String creditorName,
                           BigDecimal amount, String currency) {
        this.debtorId    = debtorId;
        this.debtorName  = debtorName;
        this.creditorId  = creditorId;
        this.creditorName = creditorName;
        this.amount      = amount;
        this.currency    = currency;
    }

    public String     getDebtorId()    { return debtorId; }
    public String     getDebtorName()  { return debtorName; }
    public String     getCreditorId()  { return creditorId; }
    public String     getCreditorName(){ return creditorName; }
    public BigDecimal getAmount()      { return amount; }
    public String     getCurrency()    { return currency; }

    @Override
    public String toString() {
        return debtorName + " owes " + creditorName + " " + amount + " " + currency;
    }
}
