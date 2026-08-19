package com.splitsmart.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Publishes inbound webhook payloads to the RabbitMQ ingestion exchange.
 *
 * <p>The producer is intentionally thin – it wraps the raw payload in a
 * lightweight envelope, assigns a correlation ID, and hands off to the
 * broker. All parsing and business logic lives in
 * {@link IngestionService} / the downstream worker.
 *
 * <p>Calling code should treat publish failures as transient and surface
 * them as HTTP 503 so that Telegram / Meta will retry the delivery.
 */
@Service
public class WebhookProducer {

    private static final Logger log = LoggerFactory.getLogger(WebhookProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public WebhookProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes a Telegram webhook update to the ingestion exchange.
     *
     * @param payload  deserialized Telegram update
     * @param groupId  SplitSmart group ID extracted from the bot command context
     * @return correlation ID assigned to this dispatch
     */
    public String publishTelegram(TelegramWebhookPayload payload, String groupId) {
        String correlationId = UUID.randomUUID().toString();

        Map<String, Object> envelope = Map.of(
                "correlationId", correlationId,
                "source",        ExtractionSource.TELEGRAM.name(),
                "groupId",       groupId != null ? groupId : "",
                "updateId",      payload.getUpdateId(),
                "text",          payload.getMessage() != null
                                     ? payload.getMessage().getText() : "",
                "senderId",      payload.getMessage() != null && payload.getMessage().getFrom() != null
                                     ? String.valueOf(payload.getMessage().getFrom().getId()) : "",
                "senderName",    payload.getMessage() != null && payload.getMessage().getFrom() != null
                                     ? payload.getMessage().getFrom().displayName() : "",
                "timestamp",     payload.getMessage() != null
                                     ? payload.getMessage().getDate() : 0L
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.INGESTION_EXCHANGE,
                RabbitMQConfig.TELEGRAM_ROUTING_KEY,
                envelope
        );

        log.info("[WebhookProducer] Telegram update {} dispatched — correlationId={}",
                payload.getUpdateId(), correlationId);
        return correlationId;
    }

    /**
     * Publishes a WhatsApp Cloud API webhook notification to the ingestion exchange.
     *
     * @param payload deserialized WhatsApp entry
     * @param groupId SplitSmart group ID resolved from the sender's phone number
     * @return correlation ID assigned to this dispatch
     */
    public String publishWhatsApp(WhatsAppWebhookPayload payload, String groupId) {
        String correlationId = UUID.randomUUID().toString();

        // Extract first message if present
        String text      = "";
        String senderId  = "";
        String senderName = "";

        if (payload.getEntry() != null && !payload.getEntry().isEmpty()) {
            var firstEntry  = payload.getEntry().get(0);
            if (firstEntry.getChanges() != null && !firstEntry.getChanges().isEmpty()) {
                var value = firstEntry.getChanges().get(0).getValue();
                if (value.getMessages() != null && !value.getMessages().isEmpty()) {
                    var msg = value.getMessages().get(0);
                    senderId = msg.getFrom();
                    if (msg.getText() != null) text = msg.getText().getBody();
                }
                if (value.getContacts() != null && !value.getContacts().isEmpty()) {
                    var contact = value.getContacts().get(0);
                    if (contact.getProfile() != null) senderName = contact.getProfile().getName();
                }
            }
        }

        Map<String, Object> envelope = Map.of(
                "correlationId", correlationId,
                "source",        ExtractionSource.WHATSAPP.name(),
                "groupId",       groupId != null ? groupId : "",
                "text",          text,
                "senderId",      senderId,
                "senderName",    senderName
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.INGESTION_EXCHANGE,
                RabbitMQConfig.WHATSAPP_ROUTING_KEY,
                envelope
        );

        log.info("[WebhookProducer] WhatsApp payload dispatched — correlationId={}", correlationId);
        return correlationId;
    }
}
