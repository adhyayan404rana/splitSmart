package com.splitsmart.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * RabbitMQ consumer that dequeues webhook envelopes from
 * {@code splitsmart.webhook.queue} and drives the ingestion pipeline.
 *
 * <h3>Retry / DLQ strategy</h3>
 * <ul>
 *   <li>Transient failures (network blips, timeouts) cause the message to be
 *       nacked without requeue so the broker routes it to the Dead-Letter Queue
 *       ({@code splitsmart.webhook.dlq}) configured in {@link RabbitMQConfig}.</li>
 *   <li>Permanent failures (malformed envelopes, parse errors that cannot be
 *       retried) are acked and logged for ops inspection.</li>
 *   <li>Successful processing is always acked.</li>
 * </ul>
 *
 * <p>Concurrency is controlled by the {@code prefetchCount=10} setting on the
 * listener container factory (see {@link RabbitMQConfig}).
 */
@Component
public class IngestionWorker {

    private static final Logger log = LoggerFactory.getLogger(IngestionWorker.class);

    /** Max retry attempts tracked in the message header before permanent failure. */
    private static final int MAX_RETRIES = 3;

    private final IngestionService   ingestionService;
    private final NlpPipelineEngine  nlpPipelineEngine;
    private final RabbitTemplate     rabbitTemplate;

    public IngestionWorker(IngestionService ingestionService,
                           NlpPipelineEngine nlpPipelineEngine,
                           RabbitTemplate rabbitTemplate) {
        this.ingestionService  = ingestionService;
        this.nlpPipelineEngine = nlpPipelineEngine;
        this.rabbitTemplate    = rabbitTemplate;
    }

    // ─── Main consumer ───────────────────────────────────────────────────────

    /**
     * Listens on {@code splitsmart.webhook.queue} for inbound envelope maps.
     *
     * <p>The method signature uses {@code Map<String, Object>} matching the
     * Jackson-serialized envelope published by {@link WebhookProducer}.
     *
     * @param envelope deserialized message body
     */
    @RabbitListener(queues = RabbitMQConfig.WEBHOOK_QUEUE,
                    containerFactory = "rabbitListenerContainerFactory")
    public void onMessage(Map<String, Object> envelope) {
        String correlationId = safeGet(envelope, "correlationId", "unknown");
        String source        = safeGet(envelope, "source", "SYNTHETIC");
        String groupId       = safeGet(envelope, "groupId", "");
        String rawText       = safeGet(envelope, "text", "");

        log.info("[IngestionWorker] Received envelope correlationId={} source={} groupId={}",
                correlationId, source, groupId);

        try {
            // ── Delegate to IngestionService (dedup + pipeline + persist) ──
            IngestionService.IngestionResult result = ingestionService.process(envelope);

            switch (result.getStatus()) {
                case "SUCCESS" ->
                    log.info("[IngestionWorker] Draft created — draftId={} confidence={} tier={} correlationId={}",
                            result.getDraftId(), result.getConfidence(), result.getTier(), correlationId);
                case "SKIPPED" ->
                    log.info("[IngestionWorker] Message skipped — reason={} correlationId={}",
                            result.getReason(), correlationId);
                case "FAILED"  ->
                    log.warn("[IngestionWorker] Pipeline failed — reason={} correlationId={}",
                            result.getReason(), correlationId);
                default ->
                    log.warn("[IngestionWorker] Unknown status={} correlationId={}", result.getStatus(), correlationId);
            }

        } catch (TransientIngestionException e) {
            // Transient — let broker route to DLQ for retry
            log.error("[IngestionWorker] Transient failure for correlationId={}: {} — nacking to DLQ",
                    correlationId, e.getMessage());
            throw e;   // spring-amqp propagates unchecked exceptions as nack

        } catch (Exception e) {
            // Permanent — ack and log; re-throwing would cause infinite DLQ loop
            log.error("[IngestionWorker] Permanent failure for correlationId={}: {} — acking and discarding",
                    correlationId, e.getMessage(), e);
            // Message is acked by successful method return
        }
    }

    // ─── DLQ consumer ────────────────────────────────────────────────────────

    /**
     * Consumes messages that have exceeded the main queue TTL and landed in
     * the Dead-Letter Queue. Logs for ops alerting; does not retry further.
     */
    @RabbitListener(queues = RabbitMQConfig.WEBHOOK_DLQ,
                    containerFactory = "rabbitListenerContainerFactory")
    public void onDeadLetter(Map<String, Object> envelope) {
        String correlationId = safeGet(envelope, "correlationId", "unknown");
        String source        = safeGet(envelope, "source", "?");
        log.error("[IngestionWorker][DLQ] Message dead-lettered — correlationId={} source={} — manual review required",
                correlationId, source);
        // Future: push to ops alert channel (PagerDuty / Slack webhook)
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private static String safeGet(Map<String, Object> map, String key, String fallback) {
        Object val = map.get(key);
        return val != null ? val.toString() : fallback;
    }

    // ─── Transient exception marker ──────────────────────────────────────────

    /**
     * Thrown by downstream services to signal retriable failures
     * (e.g. Redis unavailable, DB connection timeout).
     * Spring-AMQP converts unchecked exceptions to message nacks.
     */
    public static class TransientIngestionException extends RuntimeException {
        public TransientIngestionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
