package com.splitsmart.settlement;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read DTO containing a generated UPI payment intent for a simplified debt.
 *
 * <p>Returned by {@code POST /groups/{id}/payment-intent}. The frontend
 * {@code SettlementScreen} uses the {@code upiIntentUri} to construct a
 * deep-link button that opens the user's default UPI app (GPay, PhonePe,
 * Paytm, etc.) with the amount and receiver pre-filled.
 *
 * <p>The {@code qrCodeBase64} field contains a Base64-encoded PNG of a
 * QR code encoding the same UPI intent string, suitable for display in
 * an {@code <img>} tag when the payer cannot use a deep-link.
 */
public class PaymentIntentResponse {

    private final String     payerId;
    private final String     payerName;
    private final String     receiverId;
    private final String     receiverName;
    private final String     receiverVpa;
    private final BigDecimal amount;
    private final String     currency;

    /**
     * UPI intent URI in the format:
     * {@code upi://pay?pa=<vpa>&pn=<name>&am=<amount>&cu=INR&tn=<note>}
     */
    private final String upiIntentUri;

    /**
     * Base64-encoded PNG of the UPI QR code (data URI).
     * Prefix with {@code data:image/png;base64,} before use in an img src.
     */
    private final String qrCodeBase64;

    /** Human-readable transaction note embedded in the UPI intent. */
    private final String transactionNote;

    /** Timestamp at which this intent was generated (for TTL tracking). */
    private final Instant generatedAt;

    /** Expiry of this intent — UPI intents are valid for 30 minutes by convention. */
    private final Instant expiresAt;

    private PaymentIntentResponse(Builder b) {
        this.payerId          = b.payerId;
        this.payerName        = b.payerName;
        this.receiverId       = b.receiverId;
        this.receiverName     = b.receiverName;
        this.receiverVpa      = b.receiverVpa;
        this.amount           = b.amount;
        this.currency         = b.currency;
        this.upiIntentUri     = b.upiIntentUri;
        this.qrCodeBase64     = b.qrCodeBase64;
        this.transactionNote  = b.transactionNote;
        this.generatedAt      = b.generatedAt != null ? b.generatedAt : Instant.now();
        this.expiresAt        = b.expiresAt != null ? b.expiresAt : Instant.now().plusSeconds(1800);
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public String     getPayerId()         { return payerId; }
    public String     getPayerName()       { return payerName; }
    public String     getReceiverId()      { return receiverId; }
    public String     getReceiverName()    { return receiverName; }
    public String     getReceiverVpa()     { return receiverVpa; }
    public BigDecimal getAmount()          { return amount; }
    public String     getCurrency()        { return currency; }
    public String     getUpiIntentUri()    { return upiIntentUri; }
    public String     getQrCodeBase64()    { return qrCodeBase64; }
    public String     getTransactionNote() { return transactionNote; }
    public Instant    getGeneratedAt()     { return generatedAt; }
    public Instant    getExpiresAt()       { return expiresAt; }

    // ─── Builder ─────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String payerId; private String payerName;
        private String receiverId; private String receiverName; private String receiverVpa;
        private BigDecimal amount; private String currency;
        private String upiIntentUri; private String qrCodeBase64; private String transactionNote;
        private Instant generatedAt; private Instant expiresAt;

        private Builder() {}

        public Builder payerId(String v)          { payerId = v;         return this; }
        public Builder payerName(String v)        { payerName = v;       return this; }
        public Builder receiverId(String v)       { receiverId = v;      return this; }
        public Builder receiverName(String v)     { receiverName = v;    return this; }
        public Builder receiverVpa(String v)      { receiverVpa = v;     return this; }
        public Builder amount(BigDecimal v)       { amount = v;          return this; }
        public Builder currency(String v)         { currency = v;        return this; }
        public Builder upiIntentUri(String v)     { upiIntentUri = v;    return this; }
        public Builder qrCodeBase64(String v)     { qrCodeBase64 = v;    return this; }
        public Builder transactionNote(String v)  { transactionNote = v; return this; }
        public Builder generatedAt(Instant v)     { generatedAt = v;     return this; }
        public Builder expiresAt(Instant v)       { expiresAt = v;       return this; }

        public PaymentIntentResponse build()      { return new PaymentIntentResponse(this); }
    }
}
