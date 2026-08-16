package com.splitsmart.api;

import com.splitsmart.settlement.MarkSettledRequest;
import com.splitsmart.settlement.PaymentIntentResponse;
import com.splitsmart.settlement.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/groups/{groupId}/intents")
    public ResponseEntity<List<PaymentIntentResponse>> getPaymentIntents(@PathVariable UUID groupId) {
        return ResponseEntity.ok(paymentService.getPaymentIntentsForGroup(groupId));
    }

    @GetMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCodeImage(@RequestParam String upiIntent) {
        String base64 = paymentService.generateQrCodeBase64(upiIntent, 300, 300);
        if (base64.startsWith("data:image/png;base64,")) {
            String rawBase64 = base64.replace("data:image/png;base64,", "");
            byte[] imageBytes = java.util.Base64.getDecoder().decode(rawBase64);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(imageBytes);
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/settle")
    public ResponseEntity<Map<String, String>> markAsSettled(@Valid @RequestBody MarkSettledRequest request) {
        paymentService.processSettlement(request);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Settlement recorded in ledger and balances updated successfully."
        ));
    }
}
