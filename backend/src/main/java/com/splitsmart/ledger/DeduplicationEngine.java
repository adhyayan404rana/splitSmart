package com.splitsmart.ledger;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeduplicationEngine {

    private final DraftRepository draftRepository;

    public DeduplicationWarningResponse checkForDuplicates(DraftEntity currentDraft) {
        List<DraftEntity> existingDrafts = draftRepository.findByGroupIdOrderByCreatedAtDesc(currentDraft.getGroupId());
        List<java.util.UUID> candidateIds = new ArrayList<>();

        for (DraftEntity d : existingDrafts) {
            if (d.getId().equals(currentDraft.getId())) continue;

            // Check if amount is within ±5% margin and payer matches
            long diffCents = Math.abs(d.getTotalAmountCents() - currentDraft.getTotalAmountCents());
            double margin = currentDraft.getTotalAmountCents() * 0.05;

            if (diffCents <= margin && d.getPayerId().equals(currentDraft.getPayerId())) {
                candidateIds.add(d.getId());
            }
        }

        boolean warning = !candidateIds.isEmpty();
        String message = warning
                ? "Potential duplicate expense detected (" + candidateIds.size() + " similar draft found in group). Would you like to merge or keep separate?"
                : "No duplicate expenses detected.";

        return DeduplicationWarningResponse.builder()
                .duplicateWarning(warning)
                .warningMessage(message)
                .candidateDuplicateDraftIds(candidateIds)
                .build();
    }
}
