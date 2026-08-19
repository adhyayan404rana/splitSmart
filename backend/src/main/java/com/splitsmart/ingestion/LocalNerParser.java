package com.splitsmart.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tier-2 Named Entity Recognition (NER) parser.
 *
 * <p>In production this component loads a quantized ONNX model
 * (e.g. a fine-tuned DistilBERT / XLM-R variant) via the ONNX Runtime
 * Java API and performs entity extraction with BIO tagging for:
 * <ul>
 *   <li>B-AMOUNT / I-AMOUNT — monetary value and currency</li>
 *   <li>B-PERSON / I-PERSON — payer and participant names</li>
 *   <li>B-CATEGORY          — expense category signal</li>
 *   <li>B-SPLIT             — split modifier token</li>
 * </ul>
 *
 * <p>Target latency: 50–100 ms on CPU with a 4-thread thread pool.
 *
 * <p><b>Current implementation:</b> The ONNX Runtime dependency and the
 * serialized model file are introduced in a later sprint. This class
 * therefore falls back to an enhanced regex NER simulation that covers
 * semi-structured inputs the Tier-1 fast-path cannot handle, such as
 * multi-line chat snippets and receipts with noise tokens.
 */
@Component
public class LocalNerParser {

    private static final Logger log = LoggerFactory.getLogger(LocalNerParser.class);

    /** Confidence ceiling for this tier — always below Tier-1 certainty. */
    private static final int BASE_CONFIDENCE = 78;

    // ─── Enhanced patterns for semi-structured input ─────────────────────────

    private static final Pattern AMOUNT_MULTI = Pattern.compile(
            "(?:total|amount|paid|cost|spent|bill)[:\\s]*(?:₹|Rs\\.?\\s*|INR\\s*)?([\\d,]+(?:\\.\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FALLBACK_AMOUNT = Pattern.compile(
            "(?:₹|Rs\\.?\\s*)?([1-9][\\d,]{2,}(?:\\.\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PERSON_COLON = Pattern.compile(
            "^([A-Z][a-z]+):\\s",
            Pattern.MULTILINE
    );

    private static final Pattern SPLIT_WITH = Pattern.compile(
            "(?:split with|share with|between)\\s+([\\w,\\s&]+?)(?:\\.|,|$)",
            Pattern.CASE_INSENSITIVE
    );

    private static final List<String[]> CATEGORY_MAP = List.of(
            new String[]{"Food",      "dinner", "lunch", "breakfast", "snacks", "food",
                         "groceries", "grocery", "cafe", "restaurant", "dhaba", "chai"},
            new String[]{"Transport", "uber", "ola", "cab", "taxi", "auto", "petrol",
                         "fuel", "flight", "train", "bus", "rickshaw", "metro"},
            new String[]{"Stay",      "hotel", "airbnb", "villa", "hostel", "pg",
                         "accommodation", "room", "rent", "deposit"},
            new String[]{"Bills",     "electricity", "wifi", "internet", "gas", "water",
                         "bill", "subscription", "maintenance", "recharge"}
    );

    // ─── Parse ───────────────────────────────────────────────────────────────

    /**
     * Attempts NER-style extraction on {@code text}.
     *
     * @return populated {@link ExpenseDraft} with confidence ≤ 78, or {@code null}
     *         if extraction confidence falls below {@link ExpenseDraft#MINIMUM_CONFIDENCE}.
     */
    public ExpenseDraft parse(String text, String correlationId,
                              String groupId, ExtractionSource source,
                              String senderName) {
        if (text == null || text.isBlank()) return null;

        long startMs = System.currentTimeMillis();
        log.debug("[LocalNer] Starting NER extraction for correlationId={}", correlationId);

        // ── Amount — prefer labelled, fallback to first large number ────────
        long amountMinor = extractLabelledAmount(text);
        if (amountMinor <= 0) amountMinor = extractFallbackAmount(text);
        if (amountMinor <= 0) {
            log.debug("[LocalNer] No amount resolved — returning null. correlationId={}", correlationId);
            return null;
        }

        // ── Participants from "split with X, Y" or chat-style "Name: ..." ──
        List<String> participants = extractParticipants(text);

        // ── Payer: look for "Name: " prefix lines as chat speaker ───────────
        String payer = extractChatSpeaker(text);
        if (payer == null) payer = senderName != null && !senderName.isBlank() ? senderName : "Unknown";

        // ── Category ────────────────────────────────────────────────────────
        String category = detectCategory(text);

        // ── Split type ──────────────────────────────────────────────────────
        ExpenseDraft.SplitType splitType = detectSplitType(text);

        // ── Description — first non-empty line ──────────────────────────────
        String description = text.lines()
                .filter(l -> !l.isBlank())
                .findFirst()
                .map(l -> l.length() > 60 ? l.substring(0, 60).trim() + "…" : l.trim())
                .orElse("Group expense");

        int confidence = BASE_CONFIDENCE;
        if (participants.isEmpty()) confidence -= 4;
        if ("Unknown".equals(payer)) confidence -= 4;
        if (confidence < ExpenseDraft.MINIMUM_CONFIDENCE) return null;

        long elapsed = System.currentTimeMillis() - startMs;
        log.info("[LocalNer] NER extraction done in {}ms confidence={} correlationId={}",
                elapsed, confidence, correlationId);

        return ExpenseDraft.builder()
                .correlationId(correlationId)
                .groupId(groupId)
                .source(source)
                .extractionTier(2)
                .confidence(confidence)
                .description(description)
                .totalMinorUnits(amountMinor)
                .currencyCode("INR")
                .payerIdentifier(payer)
                .splitType(splitType)
                .participants(participants)
                .category(category)
                .rawInput(text)
                .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private long extractLabelledAmount(String text) {
        Matcher m = AMOUNT_MULTI.matcher(text);
        if (m.find()) {
            try {
                return Math.round(Double.parseDouble(m.group(1).replace(",", "")) * 100);
            } catch (NumberFormatException ignored) {}
        }
        return -1L;
    }

    private long extractFallbackAmount(String text) {
        Matcher m = FALLBACK_AMOUNT.matcher(text);
        while (m.find()) {
            try {
                double val = Double.parseDouble(m.group(1).replace(",", ""));
                if (val >= 10) return Math.round(val * 100); // skip tiny numbers like "3 items"
            } catch (NumberFormatException ignored) {}
        }
        return -1L;
    }

    private List<String> extractParticipants(String text) {
        List<String> result = new ArrayList<>();

        // "split with A, B and C"
        Matcher sw = SPLIT_WITH.matcher(text);
        if (sw.find()) {
            String[] parts = sw.group(1).split("[,&]|\\band\\b");
            for (String p : parts) {
                String name = p.trim();
                if (!name.isBlank()) result.add(toTitleCase(name));
            }
        }

        // Chat-style speakers: "Name: ..." across lines
        Matcher chat = PERSON_COLON.matcher(text);
        while (chat.find()) {
            String name = chat.group(1).trim();
            if (!result.contains(name)) result.add(name);
        }

        return result;
    }

    private String extractChatSpeaker(String text) {
        Matcher m = PERSON_COLON.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private String detectCategory(String text) {
        String lower = text.toLowerCase();
        for (String[] entry : CATEGORY_MAP) {
            for (int i = 1; i < entry.length; i++) {
                if (lower.contains(entry[i])) return entry[0];
            }
        }
        return "Bills";
    }

    private ExpenseDraft.SplitType detectSplitType(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("%")) return ExpenseDraft.SplitType.PERCENTAGE;
        if (lower.contains("exact") || lower.contains("only") || lower.contains("exclu")) {
            return ExpenseDraft.SplitType.EXACT;
        }
        return ExpenseDraft.SplitType.EQUAL;
    }

    private String toTitleCase(String s) {
        if (s == null || s.isBlank()) return s;
        s = s.trim();
        return Character.toUpperCase(s.charAt(0)) + (s.length() > 1 ? s.substring(1) : "");
    }
}
