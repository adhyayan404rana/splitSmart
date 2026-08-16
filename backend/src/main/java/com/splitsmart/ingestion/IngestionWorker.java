package com.splitsmart.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionWorker {

    private final NlpPipelineEngine nlpPipelineEngine;

    // In-memory store for pending parsed drafts (in Milestone 5, appends DraftCreated event to PostgreSQL Event Store)
    private final Map<String, ExpenseDraft> pendingDrafts = new ConcurrentHashMap<>();

    @RabbitListener(queues = RabbitMQConfig.INGESTION_QUEUE)
    public void consumeIngestionMessage(Object messagePayload) {
        log.info("IngestionWorker consumed raw payload from RabbitMQ ingestion queue: {}", messagePayload);

        String rawText = extractRawTextFromPayload(messagePayload);
        if (rawText != null && !rawText.isBlank()) {
            ExpenseDraft draft = nlpPipelineEngine.processNaturalLanguageInput(rawText);
            if (draft != null) {
                pendingDrafts.put(draft.getId().toString(), draft);
                log.info("Expense Draft created successfully via {}: ID={}, AmountCents={}, Latency={}ms",
                        draft.getExtractionSource(), draft.getId(), draft.getTotalAmountCents(), draft.getLatencyMs());
            }
        }
    }

    @RabbitListener(queues = RabbitMQConfig.DLQ_QUEUE)
    public void consumeDeadLetterMessage(Object failedPayload) {
        log.warn("IngestionWorker received failed payload from Dead Letter Queue (DLQ): {}", failedPayload);
        log.warn("User Notice Triggered: 'We couldn't understand that expense. Please enter it manually.'");
    }

    private String extractRawTextFromPayload(Object payload) {
        if (payload instanceof TelegramWebhookPayload) {
            return ((TelegramWebhookPayload) payload).getMessageText();
        } else if (payload instanceof WhatsAppWebhookPayload) {
            return ((WhatsAppWebhookPayload) payload).getChatExportContent();
        } else if (payload instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) payload;
            if (map.containsKey("messageText")) return (String) map.get("messageText");
            if (map.containsKey("chatExportContent")) return (String) map.get("chatExportContent");
        }
        return payload.toString();
    }

    public Map<String, ExpenseDraft> getPendingDrafts() {
        return pendingDrafts;
    }
}
