package com.splitsmart.ledger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftResponse {
    private UUID id;
    private UUID groupId;
    private UUID payerId;
    private String payerName;
    private long totalAmountCents;
    private String currency;
    private String description;
    private String category;
    private String splitLogic;
    private List<String> participants;
    private String status;
    private boolean isDisputed;
    private String disputeReason;
    private int version; // OCC version tag
    private Instant createdAt;
    private Instant updatedAt;
}
