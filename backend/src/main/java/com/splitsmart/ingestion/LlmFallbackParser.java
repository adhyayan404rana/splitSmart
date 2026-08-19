package com.splitsmart.ingestion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Tier-3 LLM fallback parser using a structured-output API call.
 *
 * <p>Invoked only when Tier-1 (FastPath) and Tier-2 (NER) both fail to
 * reach the minimum confidence threshold. The LLM is prompted with a
 * strict JSON schema and instructed to extract:
 * <ul>
 *   <li>amount (integer, minor units)</li>
 *   <li>currency (ISO-4217 code)</li>
 *   <li>payer (display name)</li>
 *   <li>participants (string array)</li>
 *   <li>splitType (EQUAL | EXACT | PERCENTAGE)</li>
 *   <li>category (Food | Transport | Stay | Bills)</li>
 *   <li>description (≤80 chars)</li>
 *   <li>confidence (0-100)</li>
 * </ul>
 *
 * <p>The implementation calls the OpenAI-compatible chat completions endpoint
 * ({@code /v1/chat/completions}) with {@code response_format: json_object}.
 * Any provider (OpenAI, Together, Groq, local Ollama) that exposes this
 * interface can be plugged in via {@code splitsmart.llm.base-url}.
 *
 * <p>Target latency: 800 ms – 3 s. A 5-second HTTP timeout is enforced;
 * on timeout the method returns {@code null} so the caller can log and skip.
 */
@Component
public class LlmFallbackParser {

    private static final Logger log = LoggerFactory.getLogger(LlmFallbackParser.class);

    private static final int HTTP_TIMEOUT_SECONDS = 5;
    private static final int LLM_CONFIDENCE_PENALTY = 5; // deducted from LLM-reported score

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${splitsmart.llm.base-url:http://localhost:11434}")
    private String llmBaseUrl;

    @Value("${splitsmart.llm.model:llama3}")
    private String llmModel;

    @Value("${splitsmart.llm.api-key:}")
    private String llmApiKey;

    @Value("${splitsmart.llm.enabled:false}")
    private boolean llmEnabled;

    public LlmFallbackParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                .build();
    }

    // ─── Parse ───────────────────────────────────────────────────────────────

    /**
     * Sends {@code text} to the configured LLM and parses the JSON response.
     *
     * @return populated {@link ExpenseDraft} with Tier-3 provenance, or
     *         {@code null} when LLM is disabled, times out, or returns unusable JSON.
     */
    public ExpenseDraft parse(String text, String correlationId,
                              String groupId, ExtractionSource source,
                              String senderName) {
        if (!llmEnabled) {
            log.debug("[LlmFallback] LLM disabled via config — skipping. correlationId={}", correlationId);
            return null;
        }
        if (text == null || text.isBlank()) return null;

        long startMs = System.currentTimeMillis();
        log.info("[LlmFallback] Invoking LLM tier for correlationId={}", correlationId);

        try {
            String prompt = buildPrompt(text);
            String requestBody = buildRequestBody(prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(llmBaseUrl + "/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + llmApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("[LlmFallback] LLM returned HTTP {} for correlationId={}", response.statusCode(), correlationId);
                return null;
            }

            LlmExtractionResult extracted = parseResponse(response.body());
            if (extracted == null) return null;

            int confidence = Math.max(0, extracted.confidence - LLM_CONFIDENCE_PENALTY);
            if (confidence < ExpenseDraft.MINIMUM_CONFIDENCE) {
                log.warn("[LlmFallback] LLM confidence {} below threshold for correlationId={}", confidence, correlationId);
                return null;
            }

            long elapsed = System.currentTimeMillis() - startMs;
            log.info("[LlmFallback] LLM extraction done in {}ms confidence={} correlationId={}",
                    elapsed, confidence, correlationId);

            return ExpenseDraft.builder()
                    .correlationId(correlationId)
                    .groupId(groupId)
                    .source(source)
                    .extractionTier(3)
                    .confidence(confidence)
                    .description(extracted.description != null ? extracted.description : text.substring(0, Math.min(60, text.length())))
                    .totalMinorUnits(extracted.amountMinorUnits)
                    .currencyCode(extracted.currency != null ? extracted.currency : "INR")
                    .payerIdentifier(extracted.payer != null ? extracted.payer : senderName)
                    .splitType(parseSplitType(extracted.splitType))
                    .participants(extracted.participants != null ? extracted.participants : List.of())
                    .category(extracted.category != null ? extracted.category : "Bills")
                    .rawInput(text)
                    .build();

        } catch (java.net.http.HttpTimeoutException e) {
            log.warn("[LlmFallback] LLM request timed out after {}s for correlationId={}", HTTP_TIMEOUT_SECONDS, correlationId);
        } catch (Exception e) {
            log.error("[LlmFallback] LLM call failed for correlationId={}: {}", correlationId, e.getMessage());
        }
        return null;
    }

    // ─── Prompt engineering ──────────────────────────────────────────────────

    private String buildPrompt(String text) {
        return """
               You are an expense parsing assistant for SplitSmart, a group expense app.
               Extract the following fields from the user's message and return ONLY valid JSON.
               
               JSON Schema:
               {
                 "amountMinorUnits": <integer, amount in paise/cents, e.g. 400000 for ₹4000>,
                 "currency": "<ISO-4217 code, default INR>",
                 "payer": "<name of person who paid>",
                 "participants": ["<name1>", "<name2>"],
                 "splitType": "<EQUAL|EXACT|PERCENTAGE>",
                 "category": "<Food|Transport|Stay|Bills>",
                 "description": "<brief description max 80 chars>",
                 "confidence": <integer 0-100>
               }
               
               User message:
               """ + text + """
               
               Return ONLY the JSON object, no markdown, no explanation.
               """;
    }

    private String buildRequestBody(String prompt) throws JsonProcessingException {
        Map<String, Object> body = Map.of(
                "model", llmModel,
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.1,
                "max_tokens", 300,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );
        return objectMapper.writeValueAsString(body);
    }

    private LlmExtractionResult parseResponse(String responseBody) {
        try {
            // Navigate: choices[0].message.content
            var root = objectMapper.readTree(responseBody);
            var content = root.path("choices").path(0).path("message").path("content").asText();
            return objectMapper.readValue(content, LlmExtractionResult.class);
        } catch (Exception e) {
            log.warn("[LlmFallback] Failed to parse LLM JSON response: {}", e.getMessage());
            return null;
        }
    }

    private ExpenseDraft.SplitType parseSplitType(String s) {
        if (s == null) return ExpenseDraft.SplitType.EQUAL;
        return switch (s.toUpperCase()) {
            case "EXACT"      -> ExpenseDraft.SplitType.EXACT;
            case "PERCENTAGE" -> ExpenseDraft.SplitType.PERCENTAGE;
            default           -> ExpenseDraft.SplitType.EQUAL;
        };
    }

    // ─── Inner DTO ───────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LlmExtractionResult {
        public long           amountMinorUnits;
        public String         currency;
        public String         payer;
        public List<String>   participants;
        public String         splitType;
        public String         category;
        public String         description;
        public int            confidence;
    }
}
