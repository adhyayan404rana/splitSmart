package com.splitsmart.settlement;

/**
 * Command DTO for marking a simplified debt transaction as settled.
 *
 * <p>Submitted to {@code POST /groups/{id}/settle} after a payment has
 * been completed (e.g. UPI transfer confirmed). The service appends a
 * {@code SettlementRecorded} event to the ledger and triggers the
 * {@code LedgerProjectionWorker} to update the group balance view.
 */
public class MarkSettledRequest {

    /** ID of the member who made the payment (debtor). */
    private String fromMemberId;

    /** ID of the member who received the payment (creditor). */
    private String toMemberId;

    /**
     * Amount settled in major units (e.g. 850.00 for ₹850).
     * Must be positive and ≤ the outstanding balance between the two members.
     */
    private java.math.BigDecimal amount;

    /** ISO-4217 currency code. Defaults to "INR" if omitted. */
    private String currency;

    /**
     * UTR (Unique Transaction Reference) of the UPI payment.
     * Stored in the event payload for audit purposes.
     * Optional — may be null if settled by cash or offline transfer.
     */
    private String utrReference;

    /**
     * Payment method used: "UPI", "CASH", "BANK_TRANSFER", or "OTHER".
     * Defaults to "UPI" if omitted.
     */
    private String paymentMethod;

    /** Optional free-text note (e.g. "Settled via GPay, ref: 12345"). */
    private String note;

    // ─── Getters & setters ───────────────────────────────────────────────────

    public String                  getFromMemberId()  { return fromMemberId; }
    public void                    setFromMemberId(String v) { fromMemberId = v; }

    public String                  getToMemberId()    { return toMemberId; }
    public void                    setToMemberId(String v)   { toMemberId = v; }

    public java.math.BigDecimal    getAmount()        { return amount; }
    public void                    setAmount(java.math.BigDecimal v) { amount = v; }

    public String                  getCurrency()      { return currency; }
    public void                    setCurrency(String v)     { currency = v; }

    public String                  getUtrReference()  { return utrReference; }
    public void                    setUtrReference(String v) { utrReference = v; }

    public String                  getPaymentMethod() { return paymentMethod; }
    public void                    setPaymentMethod(String v){ paymentMethod = v; }

    public String                  getNote()          { return note; }
    public void                    setNote(String v)  { note = v; }
}
