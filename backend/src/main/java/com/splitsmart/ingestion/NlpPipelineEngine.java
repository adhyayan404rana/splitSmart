package com.splitsmart.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NlpPipelineEngine {

    private final FastPathParser fastPathParser;
    private final LocalNerParser localNerParser;
    private final LlmFallbackParser llmFallbackParser;

    public ExpenseDraft processNaturalLanguageInput(String input) {
        long startTime = System.currentTimeMillis();
        ExpenseDraft draft = null;

        // Tier 1: Fast Path Heuristics (< 1ms)
        draft = fastPathParser.parse(input);
        if (draft != null) {
            log.info("NLP Pipeline Tier 1 (FAST_PATH) hit in {} ms", System.currentTimeMillis() - startTime);
            return finalizeDraft(draft, startTime);
        }

        // Tier 2: Local Quantized NER (~20-50ms)
        draft = localNerParser.parse(input);
        if (draft != null) {
            log.info("NLP Pipeline Tier 2 (LOCAL_NER) hit in {} ms", System.currentTimeMillis() - startTime);
            return finalizeDraft(draft, startTime);
        }

        // Tier 3: LLM Fallback with JSON Schema Enforcer
        draft = llmFallbackParser.parse(input);
        log.info("NLP Pipeline Tier 3 (LLM_FALLBACK) completed in {} ms", System.currentTimeMillis() - startTime);
        return finalizeDraft(draft, startTime);
    }

    private ExpenseDraft finalizeDraft(ExpenseDraft draft, long startTime) {
        long latency = System.currentTimeMillis() - startTime;
        draft.setId(UUID.randomUUID());
        draft.setLatencyMs(latency);
        draft.setCreatedAt(Instant.now());
        return draft;
    }
}
