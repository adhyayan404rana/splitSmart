package com.splitsmart.settlement;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.splitsmart.auth.UserEntity;
import com.splitsmart.auth.UserRepository;
import com.splitsmart.ledger.GroupBalanceEntity;
import com.splitsmart.ledger.GroupBalanceRepository;
import com.splitsmart.ledger.LedgerCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final DebtSimplificationEngine debtSimplificationEngine;
    private final GroupBalanceRepository groupBalanceRepository;
    private final UserRepository userRepository;
    private final LedgerCommandService ledgerCommandService;

    public List<PaymentIntentResponse> getPaymentIntentsForGroup(UUID groupId) {
        List<GroupBalanceEntity> balances = groupBalanceRepository.findByGroupId(groupId);

        Map<UUID, Long> netMap = new HashMap<>();
        Map<UUID, String> userNames = new HashMap<>();
        Map<UUID, String> userVpas = new HashMap<>();

        for (GroupBalanceEntity b : balances) {
            netMap.put(b.getUserId(), b.getNetBalanceCents());
            Optional<UserEntity> uOpt = userRepository.findById(b.getUserId());
            if (uOpt.isPresent()) {
                UserEntity u = uOpt.get();
                userNames.put(u.getId(), u.getFullName());
                String vpa = u.getVpa() != null && !u.getVpa().isBlank()
                        ? u.getVpa()
                        : u.getFullName().toLowerCase().replaceAll("[^a-z0-9]", "") + "@okaxis";
                userVpas.put(u.getId(), vpa);
            } else {
                String name = "Member " + b.getUserId().toString().substring(0, 4);
                userNames.put(b.getUserId(), name);
                userVpas.put(b.getUserId(), name.toLowerCase().replaceAll("[^a-z0-9]", "") + "@okaxis");
            }
        }

        // If DB has no balances yet (or sample view needed), fallback to sample names if netMap is empty
        if (netMap.isEmpty()) {
            return generateSamplePaymentIntents(groupId);
        }

        List<SimplifiedDebtResponse> simplifiedDebts = debtSimplificationEngine.simplifyDebts(netMap, userNames);
        List<PaymentIntentResponse> result = new ArrayList<>();

        for (SimplifiedDebtResponse debt : simplifiedDebts) {
            String vpa = userVpas.getOrDefault(debt.getToUserId(),
                    debt.getToUserName().toLowerCase().replaceAll("[^a-z0-9]", "") + "@okaxis");

            String upiIntent = generateUpiIntentString(
                    vpa,
                    debt.getToUserName(),
                    debt.getAmountCents(),
                    debt.getCurrency(),
                    "SplitSmart Settlement"
            );

            String qrBase64 = generateQrCodeBase64(upiIntent, 250, 250);
            String formattedAmount = String.format(Locale.US, "%.2f", debt.getAmountCents() / 100.0);
            String universalUrl = "https://pay.splitsmart.app/settle?pa=" + encode(vpa)
                    + "&am=" + formattedAmount + "&pn=" + encode(debt.getToUserName());

            result.add(PaymentIntentResponse.builder()
                    .id("settle-" + debt.getFromUserId().toString().substring(0, 4) + "-" + debt.getToUserId().toString().substring(0, 4))
                    .fromUserId(debt.getFromUserId())
                    .fromUserName(debt.getFromUserName())
                    .toUserId(debt.getToUserId())
                    .toUserName(debt.getToUserName())
                    .payeeVpa(vpa)
                    .amountCents(debt.getAmountCents())
                    .formattedAmount(formattedAmount)
                    .currency(debt.getCurrency() != null ? debt.getCurrency() : "INR")
                    .upiIntentString(upiIntent)
                    .qrCodeBase64(qrBase64)
                    .universalPaymentUrl(universalUrl)
                    .isSettled(false)
                    .build());
        }

        return result;
    }

    public String generateUpiIntentString(String vpa, String payeeName, long amountCents, String currency, String note) {
        double amount = amountCents / 100.0;
        String formattedAmount = String.format(Locale.US, "%.2f", amount);
        String cur = currency != null ? currency : "INR";
        String memo = note != null ? note : "SplitSmart Settlement";

        return String.format("upi://pay?pa=%s&pn=%s&am=%s&cu=%s&tn=%s",
                encode(vpa),
                encode(payeeName),
                formattedAmount,
                encode(cur),
                encode(memo)
        );
    }

    public String generateQrCodeBase64(String upiIntentString, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(upiIntentString, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
        } catch (Exception e) {
            log.error("Failed to generate QR code for UPI string: {}", upiIntentString, e);
            return "";
        }
    }

    public void processSettlement(MarkSettledRequest request) {
        ledgerCommandService.recordSettlement(
                request.getGroupId(),
                request.getDebtorId(),
                request.getCreditorId(),
                request.getAmountCents(),
                request.getTransactionRef()
        );
    }

    private List<PaymentIntentResponse> generateSamplePaymentIntents(UUID groupId) {
        UUID u1 = UUID.fromString("11111111-1111-1111-1111-111111111111"); // Sarah
        UUID u3 = UUID.fromString("33333333-3333-3333-3333-333333333333"); // Amit
        UUID u4 = UUID.fromString("44444444-4444-4444-4444-444444444444"); // David

        List<PaymentIntentResponse> samples = new ArrayList<>();

        // 1. David owes Sarah ₹2,000.00
        String intent1 = generateUpiIntentString("sarah@okaxis", "Sarah", 200000L, "INR", "Goa Trip Settlement");
        samples.add(PaymentIntentResponse.builder()
                .id("settle-david-sarah")
                .fromUserId(u4)
                .fromUserName("David")
                .toUserId(u1)
                .toUserName("Sarah")
                .payeeVpa("sarah@okaxis")
                .amountCents(200000L)
                .formattedAmount("2000.00")
                .currency("INR")
                .upiIntentString(intent1)
                .qrCodeBase64(generateQrCodeBase64(intent1, 250, 250))
                .universalPaymentUrl("https://pay.splitsmart.app/settle?pa=sarah%40okaxis&am=2000.00&pn=Sarah")
                .isSettled(false)
                .build());

        // 2. Amit owes Sarah ₹1,000.00
        String intent2 = generateUpiIntentString("sarah@okaxis", "Sarah", 100000L, "INR", "Goa Trip Settlement");
        samples.add(PaymentIntentResponse.builder()
                .id("settle-amit-sarah")
                .fromUserId(u3)
                .fromUserName("Amit")
                .toUserId(u1)
                .toUserName("Sarah")
                .payeeVpa("sarah@okaxis")
                .amountCents(100000L)
                .formattedAmount("1000.00")
                .currency("INR")
                .upiIntentString(intent2)
                .qrCodeBase64(generateQrCodeBase64(intent2, 250, 250))
                .universalPaymentUrl("https://pay.splitsmart.app/settle?pa=sarah%40okaxis&am=1000.00&pn=Sarah")
                .isSettled(false)
                .build());

        return samples;
    }

    private String encode(String val) {
        if (val == null) return "";
        return URLEncoder.encode(val, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
