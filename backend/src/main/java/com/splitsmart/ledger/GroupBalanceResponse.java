package com.splitsmart.ledger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupBalanceResponse {
    private UUID groupId;
    private UUID userId;
    private String userName;
    private long netBalanceCents; // Fowler Money integer cents
    private Instant updatedAt;
}
