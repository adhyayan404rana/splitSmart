package com.splitsmart.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Orchestrates the 3-tier NLP extraction cascade for incoming expense text.
 *
 * <p>Pipeline tiers (invoked in order until one succeeds):
 * <ol>
 *   <li><b>Tier 1 — FastPath</b> ({@link FastPathParser}):
 *       Deterministic regex / Aho-Corasick heuristics. &lt;5 ms.
 *       Confidence range: 82–95.</li>
 *   <li><b>Tier 2 — Local NER</b> ({@link LocalNerParser}):
 *       Enhanced pattern matching simulating a quantized ONNX NER model.
 *       50–100 ms. Confidence range: 60–78.</li>
 *   <li><b>Tier 3 — LLM Fallback</b> ({@link LlmFallbackParser}):
 *       Structured-output call to an OpenAI-compatible endpoint.
 *       800 ms–3 s. Confidence determined by model; penalized by 5 points.</li>
 * </ol>
 *
 * <p>A tier is skipped if the previous tier returns a non-null result that
 * meets {@link ExpenseDraft#MINIMUM_CONFIDENCE}. If all three tiers fail,
 * {@code null} is returned and the caller should log for manual review.
 */
@Component
public class NlpPipelineEngine {

    private static final Logger log = LoggerFactory.getLogger(NlpPipelineEngine.class);

    private final FastPathParser  fastPathParser;
    private final LocalNerParser  localNerParser;
    private final LlmFallbackParser llmFallbackParser;

    public NlpPipelineEngine(FastPathParser fastPathParser,
                             LocalNerParser localNerParser,
                             LlmFallbackParser llmFallbackParser) {
        this.fastPathParser    = fastPathParser;
        this.localNerParser    = localNerParser;
        this.llmFallbackParser = llmFallbackParser;
    }

    // ─── Primary entry point ─────────────────────────────────────────────────

    /**
     * Runs the 3-tier cascade on {@code rawText}.
     *
     * @param rawText       free-form expense description
     * @param correlationId webhook envelope correlation ID (for tracing)
     * @param groupId       target SplitSmart group ID
     * @param source        originating channel (Telegram, WhatsApp, API…)
     * @param senderName    display name of the sender, used as payer fallback
     * @return highest-confidence {@link ExpenseDraft} from the winning tier,
     *         or {@code null} if all tiers fail
     */
    public ExpenseDraft extract(String rawText,
                                String correlationId,
                                String groupId,
                                ExtractionSource source,
                                String senderName) {

        log.info("[NlpPipeline] Starting extraction — correlationId={} source={}", correlationId, source);

        // ── Tier 1 ──────────────────────────────────────────────────────────
        ExpenseDraft result = tryTier(1, () ->
                fastPathParser.parse(rawText, correlationId, groupId, source, senderName),
                correlationId);
        if (result != null) return result;

        // ── Tier 2 ──────────────────────────────────────────────────────────
        result = tryTier(2, () ->
                localNerParser.parse(rawText, correlationId, groupId, source, senderName),
                correlationId);
        if (result != null) return result;

        // ── Tier 3 ──────────────────────────────────────────────────────────
        result = tryTier(3, () ->
                llmFallbackParser.parse(rawText, correlationId, groupId, source, senderName),
                correlationId);
        if (result != null) return result;

        log.warn("[NlpPipeline] All tiers exhausted — no draft produced for correlationId={}", correlationId);
        return null;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ExpenseDraft tryTier(int tier, TierSupplier supplier, String correlationId) {
        try {
            ExpenseDraft draft = supplier.get();
            if (draft != null && draft.meetsMinimumConfidence()) {
                log.info("[NlpPipeline] Tier {} succeeded — confidence={} correlationId={}",
                        tier, draft.getConfidence(), correlationId);
                return draft;
            }
            if (draft != null) {
                log.debug("[NlpPipeline] Tier {} produced low-confidence draft ({}) — falling through",
                        tier, draft.getConfidence());
            } else {
                log.debug("[NlpPipeline] Tier {} returned null — falling through", tier);
            }
        } catch (Exception e) {
            log.error("[NlpPipeline] Tier {} threw exception for correlationId={}: {}",
                    tier, correlationId, e.getMessage(), e);
        }
        return null;
    }

    @FunctionalInterface
    private interface TierSupplier {
        ExpenseDraft get() throws Exception;
    }

    // ─── Stats helper (for metrics / audit endpoints) ────────────────────────

    /**
     * Returns a short diagnostic string describing which tier succeeded for
     * a given draft, useful for audit log annotations.
     */
    public static String tierLabel(int tier) {
        return switch (tier) {
            case 1 -> "FastPath-regex";
            case 2 -> "LocalNER-onnx";
            case 3 -> "LLM-fallback";
            default -> "unknown";
        };
    }
}
