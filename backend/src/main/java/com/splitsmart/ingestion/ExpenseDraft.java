package com.splitsmart.ingestion;

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
public class ExpenseDraft {
    private UUID id;
    private UUID groupId;
    private String payerName;
    private UUID payerId;
    
    // Fowler Money Pattern: amount stored in lowest currency unit (cents/paise)
    private long totalAmountCents;
    private String currency; // INR, USD, EUR
    private String description;
    private String category; // Food, Transport, Accommodation, Utilities, Entertainment
    
    private List<String> participants;
    private List<String> excludedParticipants;
    private String splitLogic; // EQUAL, EXACT, PERCENT
    
    private double confidenceScore; // 0.0 to 1.0
    private ExtractionSource extractionSource;
    private long latencyMs;
    
    private Instant createdAt;
}
