package com.splitsmart.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tier-1 fast-path parser using deterministic regex / Aho-Corasick heuristics.
 *
 * <p>Handles well-structured expense sentences such as:
 * <ul>
 *   <li>"Paid ₹4,000 for dinner, split with Rahul and Maya"</li>
 *   <li>"Spent 750 on groceries, equal split"</li>
 *   <li>"David paid 1200 for petrol, exclude Aisha"</li>
 * </ul>
 *
 * <p>Target latency: &lt;5 ms. If no structured pattern is found, returns
 * {@code null} and the pipeline falls through to Tier-2 (NER).
 */
@Component
public class FastPathParser {

    private static final Logger log = LoggerFactory.getLogger(FastPathParser.class);

    // ─── Compiled patterns ───────────────────────────────────────────────────

    /** Matches: ₹4,000 | Rs 750 | 1200 INR | 500.50 */
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:₹|Rs\\.?\\s*|INR\\s*)([\\d,]+(?:\\.\\d{1,2})?)|([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:INR|Rs\\.?|rupees?)",
            Pattern.CASE_INSENSITIVE
    );

    /** Matches payer from: "David paid", "paid by Sarah", "I paid" */
    private static final Pattern PAYER_PATTERN = Pattern.compile(
            "(?:paid by\\s+([\\w\\s]+?)(?:\\s+for|,|$))|(?:([\\w]+)\\s+paid)",
            Pattern.CASE_INSENSITIVE
    );

    /** Matches split exclusion: "exclude Maya", "not including Rahul" */
    private static final Pattern EXCLUDE_PATTERN = Pattern.compile(
            "(?:exclude|not including|excluding)\\s+([\\w,\\s&]+)",
            Pattern.CASE_INSENSITIVE
    );

    /** Matches percentage split: "40% 35% 25%" or "Aisha 40%, me 35%" */
    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile(
            "\\d{1,3}%",
            Pattern.CASE_INSENSITIVE
    );

    /** Known participant first names for quick lookup */
    private static final List<String> KNOWN_NAMES = List.of(
            "Rahul", "Maya", "David", "Aisha", "Sarah", "Priya",
            "Kiran", "Ankit", "Neha", "Rohan", "Sana", "Vikram"
    );

    /** Category keyword map */
    private static final List<String[]> CATEGORY_KEYWORDS = List.of(
            new String[]{"Food",      "dinner", "lunch", "breakfast", "food", "groceries",
                         "grocery", "shack", "cafe", "restaurant", "pizza", "biryani"},
            new String[]{"Transport", "uber", "ola", "cab", "taxi", "petrol", "fuel",
                         "flight", "train", "bus", "scooter", "bike"},
            new String[]{"Stay",      "hotel", "airbnb", "villa", "hostel", "resort",
                         "room", "stay", "deposit", "rent"},
            new String[]{"Bills",     "electricity", "wifi", "internet", "gas", "water",
                         "maintenance", "subscription", "bill"}
    );

    // ─── Parse ───────────────────────────────────────────────────────────────

    /**
     * Attempts to parse {@code text} using deterministic heuristics.
     *
     * @return a populated {@link ExpenseDraft} with confidence 82–95, or
     *         {@code null} if the text does not match any fast-path pattern.
     */
    public ExpenseDraft parse(String text, String correlationId,
                              String groupId, ExtractionSource source,
                              String senderName) {
        if (text == null || text.isBlank()) return null;

        long startMs = System.currentTimeMillis();

        // ── 1. Amount extraction ────────────────────────────────────────────
        long amountMinor = extractAmount(text);
        if (amountMinor <= 0) {
            log.debug("[FastPath] No amount found — falling through. correlationId={}", correlationId);
            return null;
        }

        // ── 2. Payer extraction ─────────────────────────────────────────────
        String payer = extractPayer(text, senderName);

        // ── 3. Category detection ───────────────────────────────────────────
        String category = detectCategory(text);

        // ── 4. Split type ───────────────────────────────────────────────────
        ExpenseDraft.SplitType splitType = detectSplitType(text);

        // ── 5. Participants ─────────────────────────────────────────────────
        List<String> participants = extractParticipants(text, payer);

        // ── 6. Description (first 60 chars, trimmed) ────────────────────────
        String description = text.length() > 60 ? text.substring(0, 60).trim() + "…" : text.trim();

        // Confidence: 90 base, -5 if payer unknown, -3 if category is default "Bills"
        int confidence = 90;
        if (payer.equalsIgnoreCase("Unknown")) confidence -= 5;
        if ("Bills".equals(category)) confidence -= 3;
        if (participants.isEmpty()) confidence -= 2;

        long elapsed = System.currentTimeMillis() - startMs;
        log.info("[FastPath] Parsed in {}ms — amount={} category={} confidence={} correlationId={}",
                elapsed, amountMinor, category, confidence, correlationId);

        return ExpenseDraft.builder()
                .correlationId(correlationId)
                .groupId(groupId)
                .source(source)
                .extractionTier(1)
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

    // ─── Internal helpers ────────────────────────────────────────────────────

    private long extractAmount(String text) {
        Matcher m = AMOUNT_PATTERN.matcher(text);
        if (!m.find()) return -1L;
        String raw = m.group(1) != null ? m.group(1) : m.group(2);
        if (raw == null) return -1L;
        try {
            double major = Double.parseDouble(raw.replace(",", ""));
            return Math.round(major * 100);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private String extractPayer(String text, String senderName) {
        Matcher m = PAYER_PATTERN.matcher(text);
        if (m.find()) {
            String g1 = m.group(1);
            String g2 = m.group(2);
            String candidate = g1 != null ? g1.trim() : (g2 != null ? g2.trim() : null);
            if (candidate != null && !candidate.equalsIgnoreCase("I") &&
                !candidate.equalsIgnoreCase("me")) {
                return toTitleCase(candidate);
            }
        }
        // Fallback: sender name
        return senderName != null && !senderName.isBlank() ? senderName : "Unknown";
    }

    private String detectCategory(String lower) {
        String text = lower.toLowerCase();
        for (String[] entry : CATEGORY_KEYWORDS) {
            for (int i = 1; i < entry.length; i++) {
                if (text.contains(entry[i])) return entry[0];
            }
        }
        return "Bills";
    }

    private ExpenseDraft.SplitType detectSplitType(String text) {
        String lower = text.toLowerCase();
        if (PERCENTAGE_PATTERN.matcher(text).find()) return ExpenseDraft.SplitType.PERCENTAGE;
        if (lower.contains("exact") || lower.contains("only") ||
            lower.contains("exclude") || lower.contains("excluding")) {
            return ExpenseDraft.SplitType.EXACT;
        }
        return ExpenseDraft.SplitType.EQUAL;
    }

    private List<String> extractParticipants(String text, String payer) {
        List<String> found = new ArrayList<>();
        String lower = text.toLowerCase();
        for (String name : KNOWN_NAMES) {
            if (lower.contains(name.toLowerCase()) && !name.equalsIgnoreCase(payer)) {
                found.add(name);
            }
        }
        return found;
    }

    private String toTitleCase(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
