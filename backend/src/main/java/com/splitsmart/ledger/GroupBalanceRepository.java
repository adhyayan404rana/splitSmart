package com.splitsmart.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link GroupBalanceEntity}.
 *
 * <p>Supports the CQRS read path. The {@code LedgerProjectionWorker} uses
 * these queries to upsert balance rows as new events arrive. The REST
 * controllers use the read queries to serve balance summary responses.
 */
@Repository
public interface GroupBalanceRepository extends JpaRepository<GroupBalanceEntity, String> {

    // ─── Per-group reads ─────────────────────────────────────────────────────

    /**
     * Returns all balance rows for a group, ordered by net balance descending
     * (largest creditors first). Used by the Overview screen.
     */
    List<GroupBalanceEntity> findByGroupIdOrderByNetBalanceDesc(String groupId);

    /**
     * Finds the balance row for a specific member within a group.
     */
    Optional<GroupBalanceEntity> findByGroupIdAndMemberId(String groupId, String memberId);

    /**
     * Returns all groups a member has a non-zero balance in.
     * Useful for the member's cross-group summary view.
     */
    @Query("SELECT gb FROM GroupBalanceEntity gb WHERE gb.memberId = :memberId AND gb.netBalance <> 0")
    List<GroupBalanceEntity> findNonZeroBalancesByMemberId(@Param("memberId") String memberId);

    // ─── Projection worker helpers ───────────────────────────────────────────

    /**
     * Returns the highest {@code lastEventVersion} processed for a group.
     * Used by the projection worker to determine the starting point for
     * incremental updates.
     */
    @Query("SELECT MAX(gb.lastEventVersion) FROM GroupBalanceEntity gb WHERE gb.groupId = :groupId")
    Optional<Long> findMaxLastEventVersionByGroupId(@Param("groupId") String groupId);

    /**
     * Bulk-updates the {@code memberName} for a member across all groups.
     * Called when a user updates their display name.
     */
    @Modifying
    @Query("UPDATE GroupBalanceEntity gb SET gb.memberName = :name WHERE gb.memberId = :memberId")
    int updateMemberNameByMemberId(@Param("memberId") String memberId, @Param("name") String name);

    // ─── Settlement helpers ──────────────────────────────────────────────────

    /**
     * Returns all members with a negative net balance in a group (i.e. they owe money).
     * Input to the debt simplification engine.
     */
    @Query("SELECT gb FROM GroupBalanceEntity gb WHERE gb.groupId = :groupId AND gb.netBalance < 0 " +
           "ORDER BY gb.netBalance ASC")
    List<GroupBalanceEntity> findDebtorsByGroupId(@Param("groupId") String groupId);

    /**
     * Returns all members with a positive net balance in a group (i.e. they are owed money).
     */
    @Query("SELECT gb FROM GroupBalanceEntity gb WHERE gb.groupId = :groupId AND gb.netBalance > 0 " +
           "ORDER BY gb.netBalance DESC")
    List<GroupBalanceEntity> findCreditorsByGroupId(@Param("groupId") String groupId);

    /**
     * Checks whether all members in a group have a zero net balance
     * (i.e. the group is fully settled).
     */
    @Query("SELECT COUNT(gb) = 0 FROM GroupBalanceEntity gb WHERE gb.groupId = :groupId AND gb.netBalance <> 0")
    boolean isGroupFullySettled(@Param("groupId") String groupId);

    // ─── Aggregate reads ─────────────────────────────────────────────────────

    /**
     * Returns the total amount spent (sum of totalOwed) across the group.
     */
    @Query("SELECT SUM(gb.totalOwed) FROM GroupBalanceEntity gb WHERE gb.groupId = :groupId")
    Optional<BigDecimal> sumTotalOwedByGroupId(@Param("groupId") String groupId);

    /** Counts the number of members with balance rows in a group. */
    long countByGroupId(String groupId);
}
