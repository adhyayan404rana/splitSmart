package com.splitsmart.api;

import com.splitsmart.auth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    @PostMapping("/settle")
    public ResponseEntity<Map<String, Object>> markSettled(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }

        String debtId = request.getOrDefault("debtId", "");
        String method = request.getOrDefault("method", "UPI");

        return ResponseEntity.ok(Map.of(
                "debtId", debtId,
                "settledBy", userPrincipal.getId().toString(),
                "method", method,
                "status", "SETTLEMENT_RECORDED"
        ));
    }

    @GetMapping("/intent/{debtId}")
    public ResponseEntity<Map<String, Object>> generatePaymentIntent(
            @PathVariable UUID debtId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(Map.of(
                "debtId", debtId.toString(),
                "upiIntentString", "upi://pay?pa=splitsmart@upi&pn=SplitSmart&am=0&cu=INR",
                "qrCodeBase64", "",
                "message", "Payment service pending integration"
        ));
    }

    @GetMapping("/history/group/{groupId}")
    public ResponseEntity<Map<String, Object>> getPaymentHistory(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(Map.of(
                "groupId", groupId.toString(),
                "payments", java.util.List.of(),
                "totalSettled", 0
        ));
    }
}
