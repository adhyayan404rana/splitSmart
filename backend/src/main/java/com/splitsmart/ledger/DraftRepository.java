package com.splitsmart.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link DraftEntity}.
 *
 * <p>All queries are scoped to a single group to respect the multi-tenant
 * data boundary enforced by PostgreSQL Row-Level Security (V2 migration).
 */
@Repository
public interface DraftRepository extends JpaRepository<DraftEntity, String> {

    // ─── Status-filtered queries ─────────────────────────────────────────────

    /**
     * Returns all drafts for a group with the given status, ordered newest first.
     * Used by the consensus screen to list pending drafts.
     */
    List<DraftEntity> findByGroupIdAndStatusOrderByCreatedAtDesc(
            String groupId, DraftEntity.Status status);

    /**
     * Returns all drafts for a group regardless of status, newest first.
     * Used by the audit feed.
     */
    List<DraftEntity> findByGroupIdOrderByCreatedAtDesc(String groupId);

    // ─── Pending consensus helpers ───────────────────────────────────────────

    /**
     * Returns all PENDING drafts that are about to expire (for TTL sweeper jobs).
     */
    @Query("SELECT d FROM DraftEntity d WHERE d.groupId = :groupId AND d.status = 'PENDING' " +
           "AND d.expiresAt IS NOT NULL ORDER BY d.expiresAt ASC")
    List<DraftEntity> findPendingByGroupIdOrderByExpiresAtAsc(@Param("groupId") String groupId);

    /**
     * Counts pending drafts for a group — used for dashboard badge counts.
     */
    long countByGroupIdAndStatus(String groupId, DraftEntity.Status status);

    // ─── Idempotency ─────────────────────────────────────────────────────────

    /**
     * Checks whether a draft with the given correlation ID already exists.
     * Prevents duplicate draft creation from replayed webhook deliveries.
     */
    boolean existsByCorrelationId(String correlationId);

    /**
     * Finds an existing draft by correlation ID, if any.
     */
    Optional<DraftEntity> findByCorrelationId(String correlationId);

    // ─── Payer lookup ────────────────────────────────────────────────────────

    /**
     * Returns drafts in a group where the given identifier is the payer.
     * Used to compute per-member outstanding amounts.
     */
    @Query("SELECT d FROM DraftEntity d WHERE d.groupId = :groupId AND d.payerIdentifier = :payerId " +
           "AND d.status = 'APPROVED' ORDER BY d.createdAt DESC")
    List<DraftEntity> findApprovedByGroupIdAndPayer(
            @Param("groupId") String groupId,
            @Param("payerId") String payerId);
}
