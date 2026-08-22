package com.splitsmart.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Command DTO for modifying an existing expense draft.
 *
 * <p>Any member who has not yet approved may submit a modification request.
 * On receipt, the service resets all approvals, appends a {@code DraftModified}
 * event, and notifies other members via SSE.
 *
 * <p>All fields are optional — only non-null values are applied to the draft.
 */
public class ModifyDraftRequest {

    /** Updated human-readable description. */
    private String description;

    /** Updated total amount in major units (e.g. 4000.00 for ₹4,000). */
    private BigDecimal amount;

    /** Updated currency code (ISO-4217). */
    private String currency;

    /** Updated payer identifier (display name or user ID). */
    private String payerIdentifier;

    /** Updated split strategy: EQUAL, EXACT, or PERCENTAGE. */
    private String splitType;

    /** Updated expense category: Food, Transport, Stay, or Bills. */
    private String category;

    /**
     * Updated actual transaction date.
     * Distinct from the draft creation timestamp.
     */
    private LocalDate transactionDate;

    /** Updated ordered list of participant identifiers. */
    private List<String> participants;

    /**
     * Optional free-text reason for the modification.
     * Included in the audit event payload for transparency.
     */
    private String modificationReason;

    /** ID of the user submitting the modification (set by the service layer). */
    private String requestedBy;

    // ─── Getters & setters ───────────────────────────────────────────────────

    public String       getDescription()       { return description; }
    public void         setDescription(String v){ description = v; }

    public BigDecimal   getAmount()            { return amount; }
    public void         setAmount(BigDecimal v) { amount = v; }

    public String       getCurrency()          { return currency; }
    public void         setCurrency(String v)  { currency = v; }

    public String       getPayerIdentifier()        { return payerIdentifier; }
    public void         setPayerIdentifier(String v){ payerIdentifier = v; }

    public String       getSplitType()         { return splitType; }
    public void         setSplitType(String v) { splitType = v; }

    public String       getCategory()          { return category; }
    public void         setCategory(String v)  { category = v; }

    public LocalDate    getTransactionDate()          { return transactionDate; }
    public void         setTransactionDate(LocalDate v){ transactionDate = v; }

    public List<String> getParticipants()           { return participants; }
    public void         setParticipants(List<String> v){ participants = v; }

    public String       getModificationReason()      { return modificationReason; }
    public void         setModificationReason(String v){ modificationReason = v; }

    public String       getRequestedBy()        { return requestedBy; }
    public void         setRequestedBy(String v){ requestedBy = v; }
}
