package com.splitsmart.ledger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsensusStatusResponse {
    private boolean payerApproved;
    private int debtorApprovedCount;
    private int debtorTotalCount;
    private boolean consensusReached; // true if Payer + at least 1 Debtor approved
    private Map<String, String> participantStatuses; // User ID -> Status
}
