package com.splitsmart.ingestion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramWebhookPayload {
    private Long updateId;
    private String chatId;
    private String senderId;
    private String senderName;
    private String messageText;
    private String timestamp;
    private String rawJson;
}
