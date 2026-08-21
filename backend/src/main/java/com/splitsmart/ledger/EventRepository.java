package com.splitsmart.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link EventEntity}.
 *
 * <p>All queries are scoped to a single group to respect the multi-tenant
 * data boundary enforced by PostgreSQL Row-Level Security (V2 migration).
 *
 * <p>Write operations go through {@code save()} inherited from
 * {@link JpaRepository}; the OCC uniqueness constraint on
 * ({@code group_id}, {@code version}) ensures concurrent appends fail fast
 * with a {@link org.springframework.dao.DataIntegrityViolationException}.
 */
@Repository
public interface EventRepository extends JpaRepository<EventEntity, String> {

    // ─── Head / version queries ──────────────────────────────────────────────

    /**
     * Returns the maximum version number currently stored for {@code groupId},
     * or {@link Optional#empty()} if the stream is empty (new group).
     */
    @Query("SELECT MAX(e.version) FROM EventEntity e WHERE e.groupId = :groupId")
    Optional<Long> findMaxVersionByGroupId(@Param("groupId") String groupId);

    /**
     * Returns the latest (highest-version) event for {@code groupId}.
     * Useful for reading the current aggregate head without replaying the full stream.
     */
    @Query("SELECT e FROM EventEntity e WHERE e.groupId = :groupId ORDER BY e.version DESC LIMIT 1")
    Optional<EventEntity> findHeadByGroupId(@Param("groupId") String groupId);

    // ─── Stream replay queries ───────────────────────────────────────────────

    /**
     * Returns the full ordered event stream for {@code groupId}, ascending by
     * version. Used for full aggregate replay and projection rebuilds.
     */
    List<EventEntity> findByGroupIdOrderByVersionAsc(String groupId);

    /**
     * Returns events for {@code groupId} from {@code fromVersion} (inclusive)
     * onwards. Used for incremental projection updates.
     */
    @Query("SELECT e FROM EventEntity e WHERE e.groupId = :groupId AND e.version >= :fromVersion ORDER BY e.version ASC")
    List<EventEntity> findByGroupIdAndVersionGreaterThanEqualOrderByVersionAsc(
            @Param("groupId") String groupId,
            @Param("fromVersion") long fromVersion);

    // ─── Audit / type-filtered queries ──────────────────────────────────────

    /**
     * Returns all events of a specific {@code eventType} for a group, ordered
     * by version ascending. Used by the audit feed controller.
     */
    @Query("SELECT e FROM EventEntity e WHERE e.groupId = :groupId AND e.eventType = :eventType ORDER BY e.version ASC")
    List<EventEntity> findByGroupIdAndEventTypeOrderByVersionAsc(
            @Param("groupId") String groupId,
            @Param("eventType") String eventType);

    /**
     * Returns all events triggered by a specific actor within a group.
     */
    List<EventEntity> findByGroupIdAndActorIdOrderByVersionAsc(String groupId, String actorId);

    // ─── Correlation / deduplication queries ────────────────────────────────

    /**
     * Checks whether an event with the given {@code correlationId} already
     * exists in any group stream — used by the idempotency layer to detect
     * duplicate webhook deliveries.
     */
    boolean existsByCorrelationId(String correlationId);

    /**
     * Counts total events in a group stream. Useful for lightweight health
     * checks and stream length monitoring.
     */
    long countByGroupId(String groupId);
}
