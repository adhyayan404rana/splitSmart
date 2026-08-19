package com.splitsmart.ingestion;

/**
 * Response DTO returned to Telegram / WhatsApp webhook endpoints
 * after a webhook payload has been accepted and dispatched to the
 * async ingestion queue.
 *
 * The response is intentionally minimal – the caller (Meta / Telegram)
 * only needs a 200 OK with a small acknowledgement body. Heavy processing
 * happens asynchronously in {@link IngestionService}.
 */
public class WebhookIngestResponse {

    /**
     * Unique message ID assigned by RabbitMQ (returned from
     * {@code RabbitTemplate.convertAndSend}) or a synthetic UUID if the
     * message was accepted before publishing.
     */
    private final String messageId;

    /**
     * Short human-readable status. Always "ACCEPTED" on success;
     * may be "DUPLICATE" when the idempotency layer detects a replay.
     */
    private final String status;

    /**
     * ISO-8601 timestamp at which the server accepted the webhook.
     * Useful for debugging clock-skew between Meta/Telegram and our server.
     */
    private final String acceptedAt;

    /**
     * Source platform that delivered the webhook.
     */
    private final String source;

    public WebhookIngestResponse(String messageId, String status,
                                 String acceptedAt, String source) {
        this.messageId  = messageId;
        this.status     = status;
        this.acceptedAt = acceptedAt;
        this.source     = source;
    }

    // ─── Factory helpers ────────────────────────────────────────────────────

    public static WebhookIngestResponse accepted(String messageId, String source) {
        return new WebhookIngestResponse(
                messageId,
                "ACCEPTED",
                java.time.Instant.now().toString(),
                source
        );
    }

    public static WebhookIngestResponse duplicate(String messageId, String source) {
        return new WebhookIngestResponse(
                messageId,
                "DUPLICATE",
                java.time.Instant.now().toString(),
                source
        );
    }

    // ─── Getters ────────────────────────────────────────────────────────────

    public String getMessageId()   { return messageId; }
    public String getStatus()      { return status; }
    public String getAcceptedAt()  { return acceptedAt; }
    public String getSource()      { return source; }

    @Override
    public String toString() {
        return "WebhookIngestResponse{" +
               "messageId='" + messageId + '\'' +
               ", status='" + status + '\'' +
               ", acceptedAt='" + acceptedAt + '\'' +
               ", source='" + source + '\'' +
               '}';
    }
}
