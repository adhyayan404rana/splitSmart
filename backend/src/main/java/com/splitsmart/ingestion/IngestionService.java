package com.splitsmart.ingestion;

import org.springframework.stereotype.Service;

/**
 * Ingestion & AI NLP Pipeline Module Boundary
 * Responsible for Webhook Ingress (Telegram/WhatsApp), Async Queuing, Fast-Path Regex,
 * Local Quantized NER, and LLM Structured Output Fallback.
 */
@Service
public class IngestionService {
    public String getModuleInfo() {
        return "Ingestion & AI NLP Pipeline Module Initialized";
    }
}
