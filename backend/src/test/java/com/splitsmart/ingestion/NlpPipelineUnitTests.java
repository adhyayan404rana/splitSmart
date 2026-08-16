package com.splitsmart.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:nlpdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NlpPipelineUnitTests {

    @Autowired
    private NlpPipelineEngine nlpPipelineEngine;

    @Test
    void testFastPathCommandSplit() {
        String input = "/split 500 with @alice @bob";
        ExpenseDraft draft = nlpPipelineEngine.processNaturalLanguageInput(input);

        assertNotNull(draft);
        assertEquals(ExtractionSource.FAST_PATH, draft.getExtractionSource());
        assertEquals(50000L, draft.getTotalAmountCents()); // 500.00 -> 50000 cents
        assertEquals(1.0, draft.getConfidenceScore());
        assertTrue(draft.getLatencyMs() < 50, "Fast Path execution latency must be < 50ms");
    }

    @Test
    void testFastPathExclusion() {
        String input = "Paid 4000 for dinner at shacks, exclude Maya";
        ExpenseDraft draft = nlpPipelineEngine.processNaturalLanguageInput(input);

        assertNotNull(draft);
        assertEquals(ExtractionSource.FAST_PATH, draft.getExtractionSource());
        assertEquals(400000L, draft.getTotalAmountCents()); // 4000.00 -> 400000 cents
        assertEquals("Food & Dining", draft.getCategory());
        assertTrue(draft.getExcludedParticipants().contains("Maya"));
        assertEquals(1.0, draft.getConfidenceScore());
    }

    @Test
    void testFastPathPaidSplit() {
        String input = "Paid 1500 for drinks last night, split equally with Rahul and Amit";
        ExpenseDraft draft = nlpPipelineEngine.processNaturalLanguageInput(input);

        assertNotNull(draft);
        assertEquals(ExtractionSource.FAST_PATH, draft.getExtractionSource());
        assertEquals(150000L, draft.getTotalAmountCents()); // 1500.00 -> 150000 cents
        assertTrue(draft.getParticipants().contains("Rahul"));
        assertTrue(draft.getParticipants().contains("Amit"));
    }

    @Test
    void testLatencySlaBenchmark() {
        List<Long> latencies = new ArrayList<>();
        String sampleText = "Paid 2500 for Uber cab to airport, exclude David";

        // Warm up & run 100 iterations
        for (int i = 0; i < 100; i++) {
            long start = System.nanoTime();
            ExpenseDraft draft = nlpPipelineEngine.processNaturalLanguageInput(sampleText);
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            latencies.add(durationMs);
            assertNotNull(draft);
        }

        latencies.sort(Long::compare);
        long p95Latency = latencies.get(94); // 95th percentile index

        assertTrue(p95Latency <= 3000, "p95 Latency SLA must be <= 3000ms (3.0s). Actual p95: " + p95Latency + "ms");
    }
}
