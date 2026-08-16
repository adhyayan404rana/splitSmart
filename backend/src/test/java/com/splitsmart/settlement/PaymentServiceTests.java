package com.splitsmart.settlement;

import com.splitsmart.auth.UserRepository;
import com.splitsmart.ledger.GroupBalanceRepository;
import com.splitsmart.ledger.LedgerCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTests {

    @Mock
    private DebtSimplificationEngine debtSimplificationEngine;

    @Mock
    private GroupBalanceRepository groupBalanceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LedgerCommandService ledgerCommandService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                debtSimplificationEngine,
                groupBalanceRepository,
                userRepository,
                ledgerCommandService
        );
    }

    @Test
    void testGenerateUpiIntentString_ValidInputs() {
        String intent = paymentService.generateUpiIntentString(
                "sarah@okaxis",
                "Sarah",
                150000L, // 1500.00 INR
                "INR",
                "Trip Settlement"
        );

        assertNotNull(intent);
        assertTrue(intent.startsWith("upi://pay?"));
        assertTrue(intent.contains("pa=sarah%40okaxis"));
        assertTrue(intent.contains("pn=Sarah"));
        assertTrue(intent.contains("am=1500.00"));
        assertTrue(intent.contains("cu=INR"));
        assertTrue(intent.contains("tn=Trip%20Settlement"));
    }

    @Test
    void testGenerateQrCodeBase64_ProducesValidPngDataUri() {
        String upiIntent = "upi://pay?pa=sarah@okaxis&pn=Sarah&am=500.00&cu=INR&tn=Dinner";
        String qrBase64 = paymentService.generateQrCodeBase64(upiIntent, 200, 200);

        assertNotNull(qrBase64);
        assertTrue(qrBase64.startsWith("data:image/png;base64,"));
        assertTrue(qrBase64.length() > 100);
    }

    @Test
    void testGetPaymentIntentsForGroup_FallbackSamples() {
        UUID groupId = UUID.randomUUID();
        var intents = paymentService.getPaymentIntentsForGroup(groupId);

        assertNotNull(intents);
        assertFalse(intents.isEmpty());

        PaymentIntentResponse first = intents.get(0);
        assertEquals("David", first.getFromUserName());
        assertEquals("Sarah", first.getToUserName());
        assertEquals("2000.00", first.getFormattedAmount());
        assertTrue(first.getUpiIntentString().startsWith("upi://pay?"));
        assertTrue(first.getQrCodeBase64().startsWith("data:image/png;base64,"));
    }
}
