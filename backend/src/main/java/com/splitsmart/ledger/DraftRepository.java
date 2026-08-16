package com.splitsmart.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DraftRepository extends JpaRepository<DraftEntity, UUID> {
    List<DraftEntity> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
    List<DraftEntity> findByGroupIdAndStatus(UUID groupId, String status);
}
