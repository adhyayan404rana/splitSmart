package com.splitsmart.ingestion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookIngestResponse {
    private String status; // ACCEPTED, DUPLICATE_DROPPED, REJECTED
    private String message;
    private String idempotencyKey;
    private String timestamp;
}
