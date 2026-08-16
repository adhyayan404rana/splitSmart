package com.splitsmart.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupBalanceRepository extends JpaRepository<GroupBalanceEntity, UUID> {
    List<GroupBalanceEntity> findByGroupId(UUID groupId);
    Optional<GroupBalanceEntity> findByGroupIdAndUserId(UUID groupId, UUID userId);
}
