package com.splitsmart.ingestion;

public enum ExtractionSource {
    FAST_PATH,   // Tier 1: Regex & Heuristics (< 1ms execution)
    LOCAL_NER,   // Tier 2: Local Quantized NER (~20-50ms execution)
    LLM_FALLBACK // Tier 3: Structured Output LLM with JSON Schema Enforcer
}
