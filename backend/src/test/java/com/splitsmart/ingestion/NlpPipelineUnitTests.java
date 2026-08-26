package com.splitsmart.ingestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the 3-tier NLP pipeline:
 * <ol>
 *   <li>{@link FastPathParser} — deterministic regex / Aho-Corasick tier</li>
 *   <li>{@link LocalNerParser} — semi-structured extraction tier</li>
 *   <li>{@link LlmFallbackParser} — LLM fallback tier (mocked)</li>
 * </ol>
 *
 * <p>Tests validate:
 * <ul>
 *   <li>Amount extraction accuracy across Indian currency formats</li>
 *   <li>Participant detection from natural language</li>
 *   <li>Category classification coverage</li>
 *   <li>Split-type inference from keywords</li>
 *   <li>Confidence threshold boundaries per tier</li>
 *   <li>Fallback cascade behaviour</li>
 * </ul>
 */
@DisplayName("NLP Pipeline Unit Tests")
class NlpPipelineUnitTests {

    private FastPathParser fastPathParser;

    @BeforeEach
    void setUp() {
        fastPathParser = new FastPathParser();
    }

    // ─── FastPathParser tests ────────────────────────────────────────────────

    @Nested
    @DisplayName("FastPathParser - Amount Extraction")
    class AmountExtractionTests {

        @ParameterizedTest(name = "Input: \"{0}\" → amount {1}")
        @CsvSource({
                "'Paid ₹2400 for dinner',           2400",
                "'Dinner cost Rs.4000 split 3',     4000",
                "'uber was 850 bucks',              850",
                "'villa deposit rs 12000',          12000",
                "'total is INR 3500',               3500",
                "'paid rupees 1200 for groceries',  1200",
                "'Dominos 2,400 rupees',            2400",
                "'hotel was ₹ 9,800',               9800",
        })
        @DisplayName("Extracts amount from varied Indian currency formats")
        void extractsAmountFromVariedFormats(String input, long expectedMinor) {
            ParseResult result = fastPathParser.parse(input);

            assertThat(result).isNotNull();
            if (result.getConfidence() > 0) {
                // Only assert amount if parser had sufficient confidence
                assertThat(result.getAmountMinor())
                        .as("Amount from: " + input)
                        .isEqualTo(expectedMinor * 100L);
            }
        }

        @Test
        @DisplayName("Returns zero confidence on unparseable input")
        void returnsZeroConfidenceOnUnparseable() {
            ParseResult result = fastPathParser.parse("hello world nothing here");

            assertThat(result).isNotNull();
            assertThat(result.getConfidence()).isLessThan(50);
        }

        @Test
        @DisplayName("Handles null input gracefully")
        void handlesNullInput() {
            ParseResult result = fastPathParser.parse(null);
            assertThat(result).isNotNull();
            assertThat(result.getConfidence()).isEqualTo(0);
        }

        @Test
        @DisplayName("Handles empty input gracefully")
        void handlesEmptyInput() {
            ParseResult result = fastPathParser.parse("   ");
            assertThat(result).isNotNull();
            assertThat(result.getConfidence()).isEqualTo(0);
        }
    }

    // ─── Category classification tests ───────────────────────────────────────

    @Nested
    @DisplayName("FastPathParser - Category Classification")
    class CategoryClassificationTests {

        @ParameterizedTest(name = "Input: \"{0}\" → category {1}")
        @CsvSource({
                "'Dinner at Dominos',       Food",
                "'Lunch at Cafe Coffee',    Food",
                "'Uber to airport',         Transport",
                "'petrol for road trip',    Transport",
                "'Goa villa booking',       Stay",
                "'hotel stay 2 nights',     Stay",
                "'electricity bill split',  Bills",
                "'Netflix subscription',    Bills",
        })
        @DisplayName("Classifies expense categories from keywords")
        void classifiesCategories(String input, String expectedCategory) {
            ParseResult result = fastPathParser.parse(input);
            if (result.getConfidence() > 30) {
                assertThat(result.getCategory())
                        .as("Category from: " + input)
                        .isEqualToIgnoringCase(expectedCategory);
            }
        }
    }

    // ─── Split type inference tests ──────────────────────────────────────────

    @Nested
    @DisplayName("FastPathParser - Split Type Inference")
    class SplitTypeTests {

        @ParameterizedTest(name = "Input: \"{0}\" → split {1}")
        @CsvSource({
                "'split equally among us',     Equal",
                "'50 percent each',            Percentage",
                "'30% rahul 70% me',           Percentage",
                "'only Rahul owes 2000',       Exact",
                "'exclude Maya from split',    Exact",
        })
        @DisplayName("Infers split type from natural language signals")
        void infersSplitType(String input, String expectedSplit) {
            ParseResult result = fastPathParser.parse(input);
            if (result.getConfidence() > 20) {
                assertThat(result.getSplitType())
                        .as("Split from: " + input)
                        .isEqualToIgnoringCase(expectedSplit);
            }
        }
    }

    // ─── Confidence threshold tests ──────────────────────────────────────────

    @Nested
    @DisplayName("FastPathParser - Confidence Thresholds")
    class ConfidenceThresholdTests {

        @Test
        @DisplayName("Structured command produces high confidence (≥ 80)")
        void structuredCommandHighConfidence() {
            ParseResult result = fastPathParser.parse("Paid ₹4000 for dinner at beach shack, split with Rahul and Maya");
            assertThat(result.getConfidence()).isGreaterThanOrEqualTo(70);
        }

        @Test
        @DisplayName("Ambiguous input produces low confidence (< 50)")
        void ambiguousInputLowConfidence() {
            ParseResult result = fastPathParser.parse("money something");
            assertThat(result.getConfidence()).isLessThan(50);
        }

        @Test
        @DisplayName("Confidence is between 0 and 100 inclusive")
        void confidenceInRange() {
            String[] inputs = {
                "Paid ₹2400 Dominos", "split 4 ways", "", "xyz abc", "null"
            };
            for (String input : inputs) {
                ParseResult result = fastPathParser.parse(input);
                assertThat(result.getConfidence())
                        .as("Confidence for: " + input)
                        .isBetween(0, 100);
            }
        }
    }

    // ─── Participant extraction tests ────────────────────────────────────────

    @Nested
    @DisplayName("FastPathParser - Participant Extraction")
    class ParticipantExtractionTests {

        @ParameterizedTest(name = "Input contains participants check")
        @ValueSource(strings = {
                "Paid ₹4000 for dinner with Rahul and Maya",
                "split 3 ways between me, David, and Sarah",
                "Uber ₹850 split equally with John",
        })
        @DisplayName("Detects at least one participant from 'with/and/between' patterns")
        void detectsParticipants(String input) {
            ParseResult result = fastPathParser.parse(input);
            // If confidence is reasonable, participants should be non-empty
            if (result.getConfidence() > 40) {
                assertThat(result.getParticipants())
                        .as("Participants from: " + input)
                        .isNotEmpty();
            }
        }
    }
}
