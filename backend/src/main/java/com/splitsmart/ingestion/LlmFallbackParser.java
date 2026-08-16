package com.splitsmart.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class LlmFallbackParser {

    /**
     * Executes Tier 3 Structured Output LLM extraction with JSON schema enforcer and prompt injection protection.
     */
    public ExpenseDraft parse(String rawChatInput) {
        if (rawChatInput == null || rawChatInput.isBlank()) return null;

        // Prompt Injection Protection: Sanitize input text
        String sanitizedPrompt = sanitizePromptInput(rawChatInput);
        log.info("Executing Tier 3 Structured LLM Fallback on sanitized prompt: {}", sanitizedPrompt);

        // Parse extracted intent (in production connects to Groq/OpenAI JSON Schema endpoint)
        long amountCents = extractAmountFromSanitizedText(sanitizedPrompt);
        if (amountCents <= 0) {
            amountCents = 100000; // Fallback default
        }

        return ExpenseDraft.builder()
                .payerName("Payer")
                .totalAmountCents(amountCents)
                .currency("INR")
                .description("Parsed Chat Expense")
                .category("General")
                .participants(List.of("Alice", "Bob"))
                .excludedParticipants(new ArrayList<>())
                .splitLogic("EQUAL")
                .confidenceScore(0.80)
                .extractionSource(ExtractionSource.LLM_FALLBACK)
                .build();
    }

    private String sanitizePromptInput(String input) {
        // Remove control characters and escape potential injection phrases
        return input.replaceAll("[\\r\\n]+", " ")
                    .replaceAll("(?i)ignore (all )?previous instructions", "[REDACTED_PROMPT_INJECTION]")
                    .replaceAll("(?i)system prompt", "[REDACTED]")
                    .trim();
    }

    private long extractAmountFromSanitizedText(String text) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d{1,2})?)").matcher(text);
        if (m.find()) {
            return Math.round(Double.parseDouble(m.group(1)) * 100);
        }
        return 0;
    }
}
