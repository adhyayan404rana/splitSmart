package com.splitsmart.ingestion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppWebhookPayload {
    private String groupId;
    private String senderPhone;
    private String senderName;
    private String chatExportContent;
    private String timestamp;
}
