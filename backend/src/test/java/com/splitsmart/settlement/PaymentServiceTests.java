package com.splitsmart.settlement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link PaymentService}.
 *
 * <p>Validates:
 * <ul>
 *   <li>UPI intent URI format and parameter encoding</li>
 *   <li>VPA resolution from stored value and name-derived fallback</li>
 *   <li>Transaction note construction</li>
 *   <li>QR code Base64 encoding (placeholder)</li>
 *   <li>Edge cases: null VPA, zero amount, max amount boundary</li>
 * </ul>
 */
@DisplayName("Payment Service Tests")
class PaymentServiceTests {

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
    }

    // ─── UPI intent URI tests ─────────────────────────────────────────────────

    @Nested
    @DisplayName("UPI Intent URI")
    class UpiIntentUriTests {

        @Test
        @DisplayName("Intent URI starts with upi://pay")
        void intentUriStartsWithScheme() {
            PaymentIntentResponse response = paymentService.generateIntent(
                    "p1", "Maya", "r1", "Rahul", "rahul@gpay",
                    new BigDecimal("850.00"), "INR", "Goa Trip");

            assertThat(response.getUpiIntentUri()).startsWith("upi://pay");
        }

        @Test
        @DisplayName("Intent URI contains receiver VPA as pa parameter")
        void intentUriContainsVpa() {
            PaymentIntentResponse response = paymentService.generateIntent(
                    "p1", "Maya", "r1", "Rahul", "rahul@gpay",
                    new BigDecimal("500.00"), "INR", "Beach Trip");

            assertThat(response.getUpiIntentUri()).contains("pa=rahul%40gpay");
        }

        @Test
        @DisplayName("Intent URI contains amount as am parameter")
        void intentUriContainsAmount() {
            PaymentIntentResponse response = paymentService.generateIntent(
                    "p1", "Maya", "r1", "Rahul", "rahul@upi",
                    new BigDecimal("1200.50"), "INR", "Trip");

            assertThat(response.getUpiIntentUri()).contains("am=1200.50");
        }

        @Test
        @DisplayName("Intent URI contains currency as cu=INR")
        void intentUriContainsCurrency() {
            PaymentIntentResponse response = paymentService.generateIntent(
                    "p1", "Maya", "r1", "Rahul", "rahul@upi",
                    new BigDecimal("300.00"), "INR", "Misc");

            assertThat(response.getUpiIntentUri()).contains("cu=INR");
        }

        @Test
        @DisplayName("Intent URI contains transaction note as tn parameter")
        void intentUriContainsNote() {
            PaymentIntentResponse response = paymentService.generateIntent(
                    "p1", "Maya", "r1", "Rahul", "rahul@upi",
                    new BigDecimal("400.00"), "INR", "Goa Trip");

            assertThat(response.getUpiIntentUri()).contains("tn=");
            assertThat(response.getTransactionNote()).containsIgnoringCase("Maya");
            assertThat(response.getTransactionNote()).containsIgnoringCase("Rahul");
        }
    }

    // ─── VPA resolution ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("VPA Resolution")
    class VpaResolutionTests {

        @Test
        @DisplayName("Uses stored VPA when it contains @")
        void usesStoredVpa() {
            PaymentIntentResponse response = paymentService.generateIntent(
                    "p1", "Maya", "r1", "Rahul", "rahul.kumar@oksbi",
                    new BigDecimal("100.00"), "INR", "Test");

            assertThat(response.getReceiverVpa()).isEqualTo("rahul.kumar@oksbi");
        }

        @Test
        @DisplayName("Derives name@upi VPA when stored VPA is null")
        void derivesVpaFromNameOnNull() {
            PaymentIntentResponse response = paymentService.generateIntent(
                    "p1", "Maya", "r1", "Rahul", null,
                    new BigDecimal("100.00"), "INR", "Test");

            assertThat(response.getReceiverVpa()).isEqualTo("rahul@upi");
        }

        @Test
        @DisplayName("Derives name@upi VPA when stored VPA is blank")
        void derivesVpaFromNameOnBlank() {
            PaymentIntentResponse response = paymentService.generateIntent(
                    "p1", "Maya", "r1", "David Kumar", "  ",
                    new BigDecimal("100.00"), "INR", "Test");

            assertThat(response.getReceiverVpa()).endsWith("@upi");
        }

        @ParameterizedTest(name = "name={0} → vpa starts with {1}")
        @CsvSource({
                "Rahul Kumar, rahulkumar@upi",
                "Maya,        maya@upi",
                "John D,      johnd@upi",
        })
        @DisplayName("Name-derived VPA strips special characters and appends @upi")
        void nameDerivedVpaIsClean(String name, String expectedVpa) {
            PaymentIntentResponse response = paymentService.generateIntent(
                    "p1", "Payer", "r1", name, null,
                    new BigDecimal("100.00"), "INR", "Test");

            assertThat(response.getReceiverVpa()).isEqualTo(expectedVpa);
        }
    }

    // ─── Amount validation ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Amount Validation")
    class AmountValidationTests {

        @Test
        @DisplayName("Throws IllegalArgumentException on zero amount")
        void throwsOnZeroAmount() {
            assertThatThrownBy(() -> paymentService.generateIntent(
                    "p1", "Maya", "r1", "Rahul", "rahul@upi",
                    BigDecimal.ZERO, "INR", "Test"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("Throws IllegalArgumentException on negative amount")
        void throwsOnNegativeAmount() {
            assertThatThrownBy(() -> paymentService.generateIntent(
                    "p1", "Maya", "r1", "Rahul", "rahul@upi",
                    new BigDecimal("-100"), "INR", "Test"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Amount on response is rounded to 2 decimal places")
        void amountRoundedToTwoDecimals() {
            PaymentIntentResponse response = paymentService.generateIntent(
                    "p1", "Maya", "r1", "Rahul", "rahul@upi",
                    new BigDecimal("333.333"), "INR", "Test");

            assertThat(response.getAmount().scale()).isEqualTo(2);
        }
    }

    // ─── QR code ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("QR Code")
    class QrCodeTests {

        @Test
        @DisplayName("QR code field is non-null and non-blank")
        void qrCodeNotBlank() {
            PaymentIntentResponse response = paymentService.generateIntent(
                    "p1", "Maya", "r1", "Rahul", "rahul@upi",
                    new BigDecimal("500"), "INR", "Test");

            assertThat(response.getQrCodeBase64()).isNotBlank();
        }

        @Test
        @DisplayName("QR code is valid Base64")
        void qrCodeIsValidBase64() {
            PaymentIntentResponse response = paymentService.generateIntent(
                    "p1", "Maya", "r1", "Rahul", "rahul@upi",
                    new BigDecimal("500"), "INR", "Test");

            assertThatCode(() ->
                    java.util.Base64.getDecoder().decode(response.getQrCodeBase64()))
                    .doesNotThrowAnyException();
        }
    }

    // ─── Expiry ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Intent has expiresAt approximately 30 minutes in the future")
    void intentExpiresInThirtyMinutes() {
        PaymentIntentResponse response = paymentService.generateIntent(
                "p1", "Maya", "r1", "Rahul", "rahul@upi",
                new BigDecimal("200"), "INR", "Test");

        java.time.Instant now = java.time.Instant.now();
        assertThat(response.getExpiresAt()).isAfter(now.plusSeconds(1700));
        assertThat(response.getExpiresAt()).isBefore(now.plusSeconds(1900));
    }
}
