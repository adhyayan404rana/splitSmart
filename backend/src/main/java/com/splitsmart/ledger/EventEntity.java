package com.splitsmart.ledger;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a single, immutable event in the SplitSmart
 * append-only event store.
 *
 * <p>The event store is the source of truth for all group financial activity.
 * Every mutation (expense drafted, draft approved, settlement recorded, etc.)
 * is captured as an event. The current state of any aggregate is derived by
 * replaying the event stream from version 1 to the current head.
 *
 * <h3>Immutability contract</h3>
 * Once persisted, an {@code EventEntity} row MUST never be updated or deleted.
 * The {@link #payload} column stores the full event data as a JSONB blob so
 * that schema evolution is handled at the application layer, not via DDL.
 *
 * <h3>OCC (Optimistic Concurrency Control)</h3>
 * The ({@code group_id}, {@code version}) pair has a unique database constraint.
 * A concurrent writer that tries to insert at an already-taken version number
 * will receive a {@link org.springframework.dao.DataIntegrityViolationException},
 * which the service layer translates to {@link OptimisticLockingException}.
 *
 * <p>Mapped to table {@code events} (created by Flyway V3 migration).
 */
@Entity
@Table(
    name = "events",
    indexes = {
        @Index(name = "idx_events_group_version", columnList = "group_id, version", unique = true),
        @Index(name = "idx_events_group_created", columnList = "group_id, created_at")
    }
)
public class EventEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    /** Group to which this event belongs. */
    @Column(name = "group_id", nullable = false, updatable = false)
    private String groupId;

    /**
     * Monotonically increasing sequence number within the group's event stream.
     * Starts at 1 for every group.
     */
    @Column(name = "version", nullable = false, updatable = false)
    private long version;

    /**
     * Discriminator describing what happened, e.g.:
     * {@code DraftCreated}, {@code DraftApproved}, {@code ConflictResolved},
     * {@code SettlementRecorded}, {@code MemberJoined}.
     */
    @Column(name = "event_type", nullable = false, updatable = false, length = 80)
    private String eventType;

    /**
     * ID of the user who triggered this event (for audit trail).
     * May be {@code null} for system-generated events.
     */
    @Column(name = "actor_id", updatable = false)
    private String actorId;

    /**
     * Full event payload serialized as JSONB.
     * Schema is determined by {@code eventType}; application code is
     * responsible for deserialization.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "jsonb")
    private String payload;

    /**
     * Wall-clock timestamp assigned by the database at insert time.
     * Never set by application code to avoid clock-skew issues.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Correlation ID from the inbound webhook envelope that triggered this event.
     * Used to link audit events back to their originating webhook call.
     */
    @Column(name = "correlation_id", updatable = false, length = 64)
    private String correlationId;

    // ─── Constructors ────────────────────────────────────────────────────────

    protected EventEntity() {}

    private EventEntity(Builder builder) {
        this.id            = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.groupId       = builder.groupId;
        this.version       = builder.version;
        this.eventType     = builder.eventType;
        this.actorId       = builder.actorId;
        this.payload       = builder.payload;
        this.correlationId = builder.correlationId;
    }

    // ─── Getters (no setters — immutable after construction) ─────────────────

    public String  getId()            { return id; }
    public String  getGroupId()       { return groupId; }
    public long    getVersion()       { return version; }
    public String  getEventType()     { return eventType; }
    public String  getActorId()       { return actorId; }
    public String  getPayload()       { return payload; }
    public Instant getCreatedAt()     { return createdAt; }
    public String  getCorrelationId() { return correlationId; }

    // ─── Builder ─────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String id;
        private String groupId;
        private long   version;
        private String eventType;
        private String actorId;
        private String payload;
        private String correlationId;

        private Builder() {}

        public Builder id(String v)            { id = v;            return this; }
        public Builder groupId(String v)       { groupId = v;       return this; }
        public Builder version(long v)         { version = v;       return this; }
        public Builder eventType(String v)     { eventType = v;     return this; }
        public Builder actorId(String v)       { actorId = v;       return this; }
        public Builder payload(String v)       { payload = v;       return this; }
        public Builder correlationId(String v) { correlationId = v; return this; }

        public EventEntity build() { return new EventEntity(this); }
    }

    @Override
    public String toString() {
        return "EventEntity{id='" + id + "', groupId='" + groupId +
               "', version=" + version + ", eventType='" + eventType + "'}";
    }
}
