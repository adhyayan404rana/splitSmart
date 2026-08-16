package com.splitsmart.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, UUID> {
    List<EventEntity> findByAggregateIdOrderByVersionAsc(UUID aggregateId);
    List<EventEntity> findByAggregateTypeAndAggregateIdOrderByVersionAsc(String aggregateType, UUID aggregateId);
}
