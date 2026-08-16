package com.splitsmart.ingestion;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FastPathParser {

    // Regex 1: /split 500 with @alice @bob
    private static final Pattern COMMAND_SPLIT_PATTERN = Pattern.compile(
            "^/split\\s+(\\d+(?:\\.\\d{1,2})?)\\s+with\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    // Regex 2: Paid 4000 for dinner at shacks, exclude Maya
    private static final Pattern PAID_EXCLUDE_PATTERN = Pattern.compile(
            "^(?:I\\s+)?paid\\s+(?:(?:₹|\\$|€|INR|USD|EUR)\\s*)?(\\d+(?:\\.\\d{1,2})?)\\s+for\\s+(.+?)(?:,\\s*|\\s+)exclude\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    // Regex 3: Paid 1500 for drinks last night, split equally with Rahul and Amit
    private static final Pattern PAID_SPLIT_PATTERN = Pattern.compile(
            "^(?:I\\s+)?paid\\s+(?:(?:₹|\\$|€|INR|USD|EUR)\\s*)?(\\d+(?:\\.\\d{1,2})?)\\s+for\\s+(.+?)(?:\\s+split(?:\\s+equally)?\\s+with\\s+(.+))?$", Pattern.CASE_INSENSITIVE);

    public ExpenseDraft parse(String text) {
        if (text == null || text.isBlank()) return null;
        String trimmed = text.trim();

        // 1. Check Command /split Pattern
        Matcher m1 = COMMAND_SPLIT_PATTERN.matcher(trimmed);
        if (m1.matches()) {
            double amount = Double.parseDouble(m1.group(1));
            long amountCents = Math.round(amount * 100);
            List<String> participants = parseParticipants(m1.group(2));

            return ExpenseDraft.builder()
                    .payerName("Payer")
                    .totalAmountCents(amountCents)
                    .currency(detectCurrency(trimmed))
                    .description("Expense Split")
                    .category(categorizeDescription("Expense Split"))
                    .participants(participants)
                    .excludedParticipants(new ArrayList<>())
                    .splitLogic("EQUAL")
                    .confidenceScore(1.0)
                    .extractionSource(ExtractionSource.FAST_PATH)
                    .build();
        }

        // 2. Check Paid Exclude Pattern
        Matcher m2 = PAID_EXCLUDE_PATTERN.matcher(trimmed);
        if (m2.matches()) {
            double amount = Double.parseDouble(m2.group(1));
            long amountCents = Math.round(amount * 100);
            String description = m2.group(2).trim();
            List<String> excluded = parseParticipants(m2.group(3));

            return ExpenseDraft.builder()
                    .payerName("Payer")
                    .totalAmountCents(amountCents)
                    .currency(detectCurrency(trimmed))
                    .description(description)
                    .category(categorizeDescription(description))
                    .participants(new ArrayList<>()) // Auto-includes all group members except excluded
                    .excludedParticipants(excluded)
                    .splitLogic("EQUAL")
                    .confidenceScore(1.0)
                    .extractionSource(ExtractionSource.FAST_PATH)
                    .build();
        }

        // 3. Check Paid Split Pattern
        Matcher m3 = PAID_SPLIT_PATTERN.matcher(trimmed);
        if (m3.matches()) {
            double amount = Double.parseDouble(m3.group(1));
            long amountCents = Math.round(amount * 100);
            String description = m3.group(2).trim();
            String rawParticipants = m3.group(3);
            List<String> participants = rawParticipants != null ? parseParticipants(rawParticipants) : new ArrayList<>();

            return ExpenseDraft.builder()
                    .payerName("Payer")
                    .totalAmountCents(amountCents)
                    .currency(detectCurrency(trimmed))
                    .description(description)
                    .category(categorizeDescription(description))
                    .participants(participants)
                    .excludedParticipants(new ArrayList<>())
                    .splitLogic("EQUAL")
                    .confidenceScore(0.95)
                    .extractionSource(ExtractionSource.FAST_PATH)
                    .build();
        }

        return null; // Fast Path miss -> Fall to Tier 2
    }

    private List<String> parseParticipants(String raw) {
        String cleaned = raw.replaceAll("(?i)\\band\\b", ",").replaceAll("[@]", "");
        String[] tokens = cleaned.split("[,\\s]+");
        List<String> list = new ArrayList<>();
        for (String t : tokens) {
            String trimmed = t.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    private String detectCurrency(String text) {
        if (text.contains("$") || text.toUpperCase().contains("USD")) return "USD";
        if (text.contains("€") || text.toUpperCase().contains("EUR")) return "EUR";
        return "INR";
    }

    private String categorizeDescription(String desc) {
        String lower = desc.toLowerCase();
        if (lower.contains("dinner") || lower.contains("food") || lower.contains("lunch") || lower.contains("drinks") || lower.contains("shacks")) return "Food & Dining";
        if (lower.contains("cab") || lower.contains("uber") || lower.contains("flight") || lower.contains("petrol") || lower.contains("taxi")) return "Transport";
        if (lower.contains("hotel") || lower.contains("stay") || lower.contains("airbnb") || lower.contains("rent")) return "Accommodation";
        if (lower.contains("wifi") || lower.contains("electricity") || lower.contains("bill")) return "Utilities";
        return "General";
    }
}
