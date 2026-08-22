package com.splitsmart.ledger;

import java.time.Instant;

/**
 * Read DTO representing a single entry in the group audit feed.
 *
 * <p>Returned by {@code GET /groups/{id}/audit}. Each entry corresponds to
 * one {@link EventEntity} row and is displayed in the frontend {@code AuditScreen}
 * as a chronological activity timeline.
 */
public class EventAuditResponse {

    private final String  eventId;
    private final String  groupId;
    private final long    version;
    private final String  eventType;
    private final String  actorId;
    private final String  actorName;
    private final String  payload;
    private final Instant createdAt;
    private final String  correlationId;

    /** Human-readable label derived from {@code eventType}. */
    private final String label;

    private EventAuditResponse(Builder b) {
        this.eventId       = b.eventId;
        this.groupId       = b.groupId;
        this.version       = b.version;
        this.eventType     = b.eventType;
        this.actorId       = b.actorId;
        this.actorName     = b.actorName;
        this.payload       = b.payload;
        this.createdAt     = b.createdAt;
        this.correlationId = b.correlationId;
        this.label         = resolveLabel(b.eventType);
    }

    // ─── Factory from entity ─────────────────────────────────────────────────

    public static EventAuditResponse from(EventEntity e, String actorName) {
        return new Builder()
                .eventId(e.getId())
                .groupId(e.getGroupId())
                .version(e.getVersion())
                .eventType(e.getEventType())
                .actorId(e.getActorId())
                .actorName(actorName)
                .payload(e.getPayload())
                .createdAt(e.getCreatedAt())
                .correlationId(e.getCorrelationId())
                .build();
    }

    // ─── Label resolver ──────────────────────────────────────────────────────

    private static String resolveLabel(String eventType) {
        if (eventType == null) return "Unknown event";
        return switch (eventType) {
            case "DraftCreated"       -> "Expense draft created";
            case "DraftApproved"      -> "Draft approved";
            case "DraftModified"      -> "Draft modified";
            case "DraftDisputed"      -> "Dispute raised";
            case "DraftFinalized"     -> "Draft finalized";
            case "DraftExpired"       -> "Draft expired";
            case "SettlementRecorded" -> "Settlement recorded";
            case "MemberJoined"       -> "Member joined group";
            case "ConflictResolved"   -> "Conflict resolved";
            default                   -> eventType.replaceAll("([A-Z])", " $1").trim();
        };
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public String  getEventId()       { return eventId; }
    public String  getGroupId()       { return groupId; }
    public long    getVersion()       { return version; }
    public String  getEventType()     { return eventType; }
    public String  getActorId()       { return actorId; }
    public String  getActorName()     { return actorName; }
    public String  getPayload()       { return payload; }
    public Instant getCreatedAt()     { return createdAt; }
    public String  getCorrelationId() { return correlationId; }
    public String  getLabel()         { return label; }

    // ─── Builder ─────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String eventId; private String groupId; private long version;
        private String eventType; private String actorId; private String actorName;
        private String payload; private Instant createdAt; private String correlationId;

        private Builder() {}

        public Builder eventId(String v)       { eventId = v;       return this; }
        public Builder groupId(String v)       { groupId = v;       return this; }
        public Builder version(long v)         { version = v;       return this; }
        public Builder eventType(String v)     { eventType = v;     return this; }
        public Builder actorId(String v)       { actorId = v;       return this; }
        public Builder actorName(String v)     { actorName = v;     return this; }
        public Builder payload(String v)       { payload = v;       return this; }
        public Builder createdAt(Instant v)    { createdAt = v;     return this; }
        public Builder correlationId(String v) { correlationId = v; return this; }

        public EventAuditResponse build()      { return new EventAuditResponse(this); }
    }
}
