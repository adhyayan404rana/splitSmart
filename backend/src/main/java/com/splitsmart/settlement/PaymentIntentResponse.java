package com.splitsmart.settlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponse {
    private String id;
    private UUID fromUserId;
    private String fromUserName;
    private UUID toUserId;
    private String toUserName;
    private String payeeVpa;
    private long amountCents;
    private String formattedAmount;
    private String currency;
    private String upiIntentString;
    private String qrCodeBase64;
    private String universalPaymentUrl;
    private boolean isSettled;
    private String transactionRef;
}
