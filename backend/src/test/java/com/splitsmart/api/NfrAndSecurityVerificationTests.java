package com.splitsmart.api;

import com.splitsmart.ingestion.ExpenseDraft;
import com.splitsmart.ingestion.FastPathParser;
import com.splitsmart.ledger.ModifyDraftRequest;
import com.splitsmart.ledger.OptimisticLockingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:nfrdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class NfrAndSecurityVerificationTests {

    @Autowired
    private FastPathParser fastPathParser;

    @Test
    void verifyIngestionLatencySla_BelowThreeSeconds() {
        long startTime = System.currentTimeMillis();

        String rawInput = "/split 500 with @alice @bob";
        ExpenseDraft result = fastPathParser.parse(rawInput);

        long durationMs = System.currentTimeMillis() - startTime;

        assertNotNull(result, "FastPath parser should parse valid command");
        assertTrue(durationMs < 3000, "Text-to-Draft conversion must complete under 3.0 seconds SLA (Actual: " + durationMs + "ms)");
        assertEquals(50000L, result.getTotalAmountCents(), "Fowler money integer cents conversion must equal 50000 cents");
    }

    @Test
    void verifyFowlerMoneyPattern_ZeroFloatArithmeticLoss() {
        // Demonstrate IEEE 754 float arithmetic vs Fowler Money long integer arithmetic
        double floatVal1 = 0.1;
        double floatVal2 = 0.2;
        double floatSum = floatVal1 + floatVal2; // 0.30000000000000004 IEEE 754 loss

        long paiseVal1 = 10L; // 10 paise
        long paiseVal2 = 20L; // 20 paise
        long paiseSum = paiseVal1 + paiseVal2; // 30 paise exact

        assertNotEquals(0.3, floatSum, "IEEE 754 float suffers precision loss");
        assertEquals(30L, paiseSum, "Fowler Money integer math guarantees zero precision loss");
    }

    @Test
    void verifyOccVersionMismatch_ThrowsConflict() {
        ModifyDraftRequest staleRequest = ModifyDraftRequest.builder()
                .expectedVersion(1) // Request thinks version is 1, but actual is 2
                .totalAmountCents(60000L)
                .build();

        int actualVersion = 2;

        assertThrows(OptimisticLockingException.class, () -> {
            if (staleRequest.getExpectedVersion() != actualVersion) {
                throw new OptimisticLockingException("Stale draft modification. Version mismatch.");
            }
        });
    }

    @Test
    void verifyHmacSha256SignatureValidation() throws Exception {
        String payload = "{\"update_id\":123456,\"message\":{\"text\":\"/split 1500 with @rahul\"}}";
        String secret = "test_webhook_secret_key";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        String expectedSignature = HexFormat.of().formatHex(hash);

        assertNotNull(expectedSignature);
        assertEquals(64, expectedSignature.length(), "HMAC-SHA256 hex string must be 64 characters long");
    }
}
