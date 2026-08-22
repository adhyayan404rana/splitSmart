package com.splitsmart.ledger;

/**
 * Command DTO for raising a dispute on an expense draft.
 *
 * <p>Any group member (including the payer) may dispute a draft before it
 * reaches quorum. Raising a dispute transitions the draft to
 * {@link DraftEntity.Status#DISPUTED} and appends a {@code DraftDisputed}
 * event to the group's event stream, notifying all online members via SSE.
 *
 * <p>A disputed draft can be resolved by:
 * <ul>
 *   <li>The original disputer withdrawing the dispute (revoke).</li>
 *   <li>The group reaching consensus to override the dispute.</li>
 *   <li>The draft being modified to address the concern and re-submitted.</li>
 * </ul>
 */
public class DisputeRequest {

    /**
     * Free-text reason explaining why the draft is being disputed.
     * Required — must not be blank.
     */
    private String reason;

    /**
     * ID of the user raising the dispute.
     * Set by the service layer from the authenticated principal;
     * not supplied by the client.
     */
    private String disputedBy;

    /**
     * Optional suggested correction to the amount in major units.
     * If provided, it is included in the audit event payload as context
     * for other members when reviewing the dispute.
     */
    private java.math.BigDecimal suggestedAmount;

    /**
     * Optional corrected description proposed by the disputer.
     */
    private String suggestedDescription;

    // ─── Getters & setters ───────────────────────────────────────────────────

    public String getReason()                         { return reason; }
    public void   setReason(String v)                 { reason = v; }

    public String getDisputedBy()                     { return disputedBy; }
    public void   setDisputedBy(String v)             { disputedBy = v; }

    public java.math.BigDecimal getSuggestedAmount()       { return suggestedAmount; }
    public void                 setSuggestedAmount(java.math.BigDecimal v) { suggestedAmount = v; }

    public String getSuggestedDescription()            { return suggestedDescription; }
    public void   setSuggestedDescription(String v)    { suggestedDescription = v; }
}
