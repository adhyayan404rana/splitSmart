package com.splitsmart.ingestion;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LocalNerParser {

    private static final Pattern MONEY_PATTERN = Pattern.compile("(?:₹|\\$|€|INR|USD|EUR)?\\s*(\\d+(?:\\.\\d{1,2})?)");
    private static final Pattern PAYER_PATTERN = Pattern.compile("(?i)^([A-Z][a-z]+)\\s+paid");

    public ExpenseDraft parse(String text) {
        if (text == null || text.isBlank()) return null;

        Matcher moneyMatcher = MONEY_PATTERN.matcher(text);
        if (!moneyMatcher.find()) {
            return null; // Cannot extract amount
        }

        double amount = Double.parseDouble(moneyMatcher.group(1));
        long amountCents = Math.round(amount * 100);

        String payerName = "Payer";
        Matcher payerMatcher = PAYER_PATTERN.matcher(text);
        if (payerMatcher.find()) {
            payerName = payerMatcher.group(1);
        }

        List<String> participants = new ArrayList<>();
        if (text.toLowerCase().contains("split with")) {
            String afterSplit = text.substring(text.toLowerCase().indexOf("split with") + 10);
            String[] tokens = afterSplit.replaceAll("(?i)\\band\\b", ",").split("[,\\s]+");
            for (String t : tokens) {
                if (!t.isBlank()) participants.add(t.trim());
            }
        }

        return ExpenseDraft.builder()
                .payerName(payerName)
                .totalAmountCents(amountCents)
                .currency(text.contains("$") ? "USD" : text.contains("€") ? "EUR" : "INR")
                .description("Unstructured Expense")
                .category("General")
                .participants(participants)
                .excludedParticipants(new ArrayList<>())
                .splitLogic("EQUAL")
                .confidenceScore(0.88)
                .extractionSource(ExtractionSource.LOCAL_NER)
                .build();
    }
}
