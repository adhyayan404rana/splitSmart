package com.splitsmart.ledger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeduplicationWarningResponse {
    private boolean duplicateWarning;
    private String warningMessage;
    private List<UUID> candidateDuplicateDraftIds;
}
