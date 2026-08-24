package com.splitsmart.settlement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Generates UPI payment intents and QR codes for group debt settlement.
 *
 * <h3>UPI intent URI format</h3>
 * <pre>
 *   upi://pay?pa={vpa}&pn={name}&am={amount}&cu=INR&tn={note}
 * </pre>
 * This URI opens the system UPI picker on Android and iOS, pre-filling the
 * receiver, amount, and transaction note. All standard UPI apps (GPay,
 * PhonePe, Paytm, BHIM) support this scheme.
 *
 * <h3>QR code</h3>
 * The QR code encodes the same UPI intent URI as a Base64 PNG.
 * In the current implementation a lightweight ASCII-art placeholder is
 * Base64-encoded; the production sprint will integrate the ZXing library
 * for real QR matrix rendering.
 *
 * <h3>VPA resolution</h3>
 * VPAs are stored on the {@code GroupBalanceEntity#memberName} field in the
 * format {@code name@upi} as a convention. In production these would be
 * verified against the NPCI VPA lookup API.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    /** Maximum amount that can be transferred in a single UPI transaction (₹1 lakh). */
    private static final BigDecimal UPI_MAX_AMOUNT = new BigDecimal("100000.00");

    // ─── Payment intent generation ───────────────────────────────────────────

    /**
     * Generates a UPI payment intent for the given debt edge.
     *
     * @param payerId      ID of the member who owes money
     * @param payerName    display name of the payer
     * @param receiverId   ID of the member to be paid
     * @param receiverName display name of the receiver
     * @param receiverVpa  UPI VPA of the receiver (e.g. "rahul@gpay")
     * @param amount       amount to transfer in major units
     * @param currency     ISO-4217 currency code (typically "INR")
     * @param groupName    group name, included in the transaction note
     * @return populated {@link PaymentIntentResponse}
     */
    public PaymentIntentResponse generateIntent(String payerId, String payerName,
                                                String receiverId, String receiverName,
                                                String receiverVpa, BigDecimal amount,
                                                String currency, String groupName) {
        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (amount.compareTo(UPI_MAX_AMOUNT) > 0) {
            log.warn("[PaymentService] Amount {} exceeds UPI single-txn limit — splitting not yet supported", amount);
        }

        String formattedAmount = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String vpa = resolveVpa(receiverVpa, receiverName);
        String note = buildTransactionNote(payerName, receiverName, groupName);
        String intentUri = buildUpiIntentUri(vpa, receiverName, formattedAmount, currency, note);
        String qrCode = generateQrCodeBase64(intentUri);

        log.info("[PaymentService] Intent generated — payer={} receiver={} amount={} vpa={}",
                payerName, receiverName, formattedAmount, vpa);

        return PaymentIntentResponse.builder()
                .payerId(payerId)
                .payerName(payerName)
                .receiverId(receiverId)
                .receiverName(receiverName)
                .receiverVpa(vpa)
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .currency(currency != null ? currency : "INR")
                .upiIntentUri(intentUri)
                .qrCodeBase64(qrCode)
                .transactionNote(note)
                .build();
    }

    // ─── UPI intent URI builder ──────────────────────────────────────────────

    private String buildUpiIntentUri(String vpa, String payeeName,
                                     String amount, String currency, String note) {
        return "upi://pay" +
               "?pa=" + encode(vpa) +
               "&pn=" + encode(payeeName) +
               "&am=" + encode(amount) +
               "&cu=" + encode(currency != null ? currency : "INR") +
               "&tn=" + encode(note);
    }

    private String buildTransactionNote(String payer, String receiver, String groupName) {
        String group = groupName != null && !groupName.isBlank() ? groupName : "SplitSmart";
        return payer + " settles with " + receiver + " via " + group;
    }

    // ─── VPA resolution ──────────────────────────────────────────────────────

    /**
     * Resolves a UPI VPA from stored value or derives a placeholder.
     * Convention: "name@upi" is the fallback for demo environments.
     */
    private String resolveVpa(String storedVpa, String name) {
        if (storedVpa != null && !storedVpa.isBlank() && storedVpa.contains("@")) {
            return storedVpa.trim().toLowerCase();
        }
        // Derive a placeholder VPA from the member's name
        String clean = name != null
                ? name.toLowerCase().replaceAll("[^a-z0-9]", "")
                : "member";
        return clean + "@upi";
    }

    // ─── QR code (placeholder) ───────────────────────────────────────────────

    /**
     * Encodes the UPI intent URI as a Base64 string.
     *
     * <p>Production implementation: replace this body with a ZXing
     * {@code QRCodeWriter} call that renders a 300×300 PNG matrix.
     *
     * @param upiIntentUri the full UPI intent URI to encode
     * @return Base64-encoded PNG placeholder
     */
    private String generateQrCodeBase64(String upiIntentUri) {
        // Stub: Base64-encode the URI itself as a text QR placeholder
        // ZXing integration scheduled for production sprint
        return Base64.getEncoder().encodeToString(upiIntentUri.getBytes(StandardCharsets.UTF_8));
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private static String encode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
