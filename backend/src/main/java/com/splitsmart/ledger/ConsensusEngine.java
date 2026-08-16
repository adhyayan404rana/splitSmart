package com.splitsmart.ledger;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConsensusEngine {

    /**
     * Evaluates FR-2 Consensus Rule: Draft is committed to ledger ONLY after Payer + at least one Debtor approve.
     */
    public ConsensusStatusResponse evaluateConsensus(boolean payerApproved, int debtorApprovedCount, int debtorTotalCount, Map<String, String> participantStatuses) {
        boolean consensusReached = payerApproved && (debtorTotalCount == 0 || debtorApprovedCount >= 1);

        return ConsensusStatusResponse.builder()
                .payerApproved(payerApproved)
                .debtorApprovedCount(debtorApprovedCount)
                .debtorTotalCount(debtorTotalCount)
                .consensusReached(consensusReached)
                .participantStatuses(participantStatuses)
                .build();
    }
}
