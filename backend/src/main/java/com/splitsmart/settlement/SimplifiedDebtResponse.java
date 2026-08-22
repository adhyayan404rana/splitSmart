package com.splitsmart.settlement;

import java.math.BigDecimal;

/**
 * Read DTO representing a single debt edge in the simplified debt graph.
 *
 * <p>Produced by {@link DebtSimplificationEngine} after running the
 * DP bitmask exact solver or greedy heap fallback. The simplified graph
 * minimises the total number of transactions required to settle all debts.
 *
 * <p>Each instance represents a single payment: {@code payerId} should
 * transfer {@code amount} to {@code receiverId} to settle their portion
 * of the group's shared expenses.
 *
 * <p>A UPI deep-link or QR code can be generated from this DTO by
 * {@code PaymentService}.
 */
public class SimplifiedDebtResponse {

    private final String     payerId;
    private final String     payerName;
    private final String     receiverId;
    private final String     receiverName;
    private final BigDecimal amount;
    private final String     currency;

    /** UPI VPA of the receiver, if available (populated by PaymentService). */
    private final String     receiverUpiVpa;

    /** Pre-generated UPI intent URI for deep-linking into payment apps. */
    private final String     upiIntentUri;

    public SimplifiedDebtResponse(String payerId, String payerName,
                                  String receiverId, String receiverName,
                                  BigDecimal amount, String currency,
                                  String receiverUpiVpa, String upiIntentUri) {
        this.payerId        = payerId;
        this.payerName      = payerName;
        this.receiverId     = receiverId;
        this.receiverName   = receiverName;
        this.amount         = amount;
        this.currency       = currency;
        this.receiverUpiVpa = receiverUpiVpa;
        this.upiIntentUri   = upiIntentUri;
    }

    public String     getPayerId()        { return payerId; }
    public String     getPayerName()      { return payerName; }
    public String     getReceiverId()     { return receiverId; }
    public String     getReceiverName()   { return receiverName; }
    public BigDecimal getAmount()         { return amount; }
    public String     getCurrency()       { return currency; }
    public String     getReceiverUpiVpa() { return receiverUpiVpa; }
    public String     getUpiIntentUri()   { return upiIntentUri; }

    @Override
    public String toString() {
        return payerName + " → " + receiverName + " " + amount + " " + currency;
    }
}
