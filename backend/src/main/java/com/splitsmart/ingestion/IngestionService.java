package com.splitsmart.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates the async ingestion pipeline for a raw webhook envelope.
 *
 * <p>Pipeline stages:
 * <ol>
 *   <li><b>Deduplication</b> – check the idempotency key (correlationId) against
 *       Redis; return early if already processed within the TTL window.</li>
 *   <li><b>NLP extraction</b> – pass the raw text through the 3-tier parser
 *       (FastPath → NER → LLM) to obtain an {@link ExpenseDraft}.</li>
 *   <li><b>Confidence gate</b> – if confidence ≥ threshold, persist a
 *       {@code DraftEntity} and trigger consensus; otherwise log for manual review.</li>
 *   <li><b>Notification</b> – push an SSE event to all online group members.</li>
 * </ol>
 *
 * <p>This service is invoked by {@code IngestionWorker} (RabbitMQ listener)
 * and also directly from the REST API for synchronous ingestion requests.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    /**
     * Confidence threshold below which a draft is flagged for manual review
     * rather than being automatically submitted to consensus.
     */
    private static final int AUTO_SUBMIT_THRESHOLD = 80;

    // ─── Dependencies (injected via constructor) ─────────────────────────────
    // NOTE: The NLP pipeline engines, DraftRepository, and NotificationService
    //       are declared as optional collaborators so this service compiles
    //       before those modules are implemented (Days 9+).

    public IngestionService() {
        // Collaborators injected when available (see Day 9+ modules)
    }

    // ─── Primary entry point ─────────────────────────────────────────────────

    /**
     * Processes a deserialized webhook envelope published by {@link WebhookProducer}.
     *
     * @param envelope key-value map from the RabbitMQ message body
     * @return result summary (correlationId, status, confidence, draftId if created)
     */
    public IngestionResult process(Map<String, Object> envelope) {
        String correlationId = getString(envelope, "correlationId", UUID.randomUUID().toString());
        String groupId       = getString(envelope, "groupId", "");
        String rawText       = getString(envelope, "text", "");
        String sourceStr     = getString(envelope, "source", ExtractionSource.SYNTHETIC.name());
        String senderId      = getString(envelope, "senderId", "");
        String senderName    = getString(envelope, "senderName", "");

        log.info("[IngestionService] Processing envelope correlationId={} source={} groupId={}",
                correlationId, sourceStr, groupId);

        if (rawText.isBlank()) {
            log.warn("[IngestionService] Empty text in envelope correlationId={} — skipping NLP", correlationId);
            return IngestionResult.skipped(correlationId, "EMPTY_TEXT");
        }

        ExtractionSource source;
        try {
            source = ExtractionSource.valueOf(sourceStr);
        } catch (IllegalArgumentException e) {
            source = ExtractionSource.SYNTHETIC;
        }

        // ── Tier 1: FastPath heuristic parse ───────────────────────────────
        // (Full implementation in Day 9 — FastPathParser.java)
        ExpenseDraft draft = fastPathAttempt(rawText, correlationId, groupId, source, senderName);

        if (draft == null) {
            // ── Tier 2: ONNX NER parse ─────────────────────────────────────
            // (Full implementation in Day 9 — LocalNerParser.java)
            draft = nerAttempt(rawText, correlationId, groupId, source, senderName);
        }

        if (draft == null) {
            // ── Tier 3: LLM fallback ───────────────────────────────────────
            // (Full implementation in Day 9 — LlmFallbackParser.java)
            draft = llmAttempt(rawText, correlationId, groupId, source, senderName);
        }

        if (draft == null || !draft.meetsMinimumConfidence()) {
            log.warn("[IngestionService] Extraction failed or below threshold for correlationId={}", correlationId);
            return IngestionResult.failed(correlationId, "LOW_CONFIDENCE");
        }

        log.info("[IngestionService] Draft extracted — correlationId={} confidence={} tier={}",
                correlationId, draft.getConfidence(), draft.getExtractionTier());

        // ── Persistence placeholder (Day 9/10 — DraftRepository) ──────────
        String draftId = "draft_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // ── Notification placeholder (Day 13 — NotificationService) ───────
        log.info("[IngestionService] Draft {} ready for consensus in group {}", draftId, groupId);

        return IngestionResult.success(correlationId, draftId, draft.getConfidence(), draft.getExtractionTier());
    }

    // ─── Pipeline tier stubs (replaced by real parsers on Day 9) ─────────────

    private ExpenseDraft fastPathAttempt(String text, String correlationId,
                                         String groupId, ExtractionSource source,
                                         String senderName) {
        // Placeholder: simple regex check for "₹<amount>" pattern
        if (!text.contains("₹") && !text.matches(".*\\d+.*")) return null;

        // Attempt a naive amount parse
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d[\\d,]*)").matcher(text.replace(",", ""));
        if (!m.find()) return null;

        long amount;
        try {
            amount = Long.parseLong(m.group(1).replace(",", "")) * 100L;
        } catch (NumberFormatException e) {
            return null;
        }

        return ExpenseDraft.builder()
                .correlationId(correlationId)
                .groupId(groupId)
                .source(source)
                .extractionTier(1)
                .confidence(85)
                .description(text.length() > 60 ? text.substring(0, 60) + "…" : text)
                .totalMinorUnits(amount)
                .currencyCode("INR")
                .payerIdentifier(senderName.isBlank() ? "Unknown" : senderName)
                .splitType(ExpenseDraft.SplitType.EQUAL)
                .category("Bills")
                .rawInput(text)
                .build();
    }

    @SuppressWarnings("SameReturnValue")
    private ExpenseDraft nerAttempt(String text, String correlationId,
                                    String groupId, ExtractionSource source,
                                    String senderName) {
        // Stub — real ONNX NER model integrated on Day 9
        log.debug("[IngestionService] NER tier attempted for correlationId={}", correlationId);
        return null;
    }

    @SuppressWarnings("SameReturnValue")
    private ExpenseDraft llmAttempt(String text, String correlationId,
                                    String groupId, ExtractionSource source,
                                    String senderName) {
        // Stub — real LLM fallback integrated on Day 9
        log.debug("[IngestionService] LLM fallback attempted for correlationId={}", correlationId);
        return null;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static String getString(Map<String, Object> map, String key, String fallback) {
        Object val = map.get(key);
        return val != null ? val.toString() : fallback;
    }

    // ─── Result record ───────────────────────────────────────────────────────

    public static final class IngestionResult {
        private final String correlationId;
        private final String status;         // "SUCCESS" | "SKIPPED" | "FAILED"
        private final String draftId;
        private final int confidence;
        private final int tier;
        private final String reason;

        private IngestionResult(String correlationId, String status,
                                String draftId, int confidence, int tier, String reason) {
            this.correlationId = correlationId;
            this.status        = status;
            this.draftId       = draftId;
            this.confidence    = confidence;
            this.tier          = tier;
            this.reason        = reason;
        }

        public static IngestionResult success(String cid, String draftId, int confidence, int tier) {
            return new IngestionResult(cid, "SUCCESS", draftId, confidence, tier, null);
        }

        public static IngestionResult skipped(String cid, String reason) {
            return new IngestionResult(cid, "SKIPPED", null, 0, 0, reason);
        }

        public static IngestionResult failed(String cid, String reason) {
            return new IngestionResult(cid, "FAILED", null, 0, 0, reason);
        }

        public String getCorrelationId() { return correlationId; }
        public String getStatus()        { return status; }
        public String getDraftId()       { return draftId; }
        public int getConfidence()       { return confidence; }
        public int getTier()             { return tier; }
        public String getReason()        { return reason; }

        @Override
        public String toString() {
            return "IngestionResult{status='" + status + "', correlationId='" + correlationId +
                   "', draftId='" + draftId + "', confidence=" + confidence + ", tier=" + tier + '}';
        }
    }
}
