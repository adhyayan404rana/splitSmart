package com.splitsmart.settlement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:debtdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DebtSimplificationUnitTests {

    @Autowired
    private DebtSimplificationEngine debtSimplificationEngine;

    private UUID uAlice;
    private UUID uBob;
    private UUID uCharlie;
    private UUID uDavid;
    private Map<UUID, String> userNames;

    @BeforeEach
    void setUp() {
        uAlice = UUID.randomUUID();
        uBob = UUID.randomUUID();
        uCharlie = UUID.randomUUID();
        uDavid = UUID.randomUUID();

        userNames = Map.of(
                uAlice, "Alice",
                uBob, "Bob",
                uCharlie, "Charlie",
                uDavid, "David"
        );
    }

    @Test
    void testCircularDebtCycleCancellation() {
        // Alice owes Bob 1500, Bob owes Charlie 1500, Charlie owes Alice 1500
        List<RawDebtResponse> rawDebts = List.of(
                new RawDebtResponse(uAlice, "Alice", uBob, "Bob", 150000L, "INR"),
                new RawDebtResponse(uBob, "Bob", uCharlie, "Charlie", 150000L, "INR"),
                new RawDebtResponse(uCharlie, "Charlie", uAlice, "Alice", 150000L, "INR")
        );

        DebtGraphComparisonResponse comparison = debtSimplificationEngine.computeComparison(rawDebts, userNames);

        assertEquals(3, comparison.getRawTransactionCount());
        assertEquals(0, comparison.getSimplifiedTransactionCount(), "Circular debt cycle must simplify to 0 transactions");
        assertEquals(100.0, comparison.getReductionPercentage(), "Transaction reduction percentage should be 100%");
    }

    @Test
    void testComplexMultiMemberDebtSimplification() {
        // 6 raw debts across 4 users
        List<RawDebtResponse> rawDebts = List.of(
                new RawDebtResponse(uBob, "Bob", uAlice, "Alice", 150000L, "INR"),
                new RawDebtResponse(uCharlie, "Charlie", uBob, "Bob", 150000L, "INR"),
                new RawDebtResponse(uAlice, "Alice", uCharlie, "Charlie", 150000L, "INR"),
                new RawDebtResponse(uDavid, "David", uAlice, "Alice", 200000L, "INR"),
                new RawDebtResponse(uCharlie, "Charlie", uDavid, "David", 100000L, "INR"),
                new RawDebtResponse(uBob, "Bob", uDavid, "David", 100000L, "INR")
        );

        DebtGraphComparisonResponse comparison = debtSimplificationEngine.computeComparison(rawDebts, userNames);

        assertEquals(6, comparison.getRawTransactionCount());
        assertTrue(comparison.getSimplifiedTransactionCount() <= 2, "6 raw debts must reduce to at most 2 minimal transactions");
        assertTrue(comparison.getReductionPercentage() >= 66.67, "Reduction percentage must be >= 66.67%");
    }

    @Test
    void testNetBalanceConservationAssertion() {
        Map<UUID, Long> netBalances = Map.of(
                uAlice, 300000L,  // +3000 INR
                uBob, -100000L,   // -1000 INR
                uCharlie, -100000L, // -1000 INR
                uDavid, -100000L  // -1000 INR
        );

        // Assert net sum is 0 before simplification
        long sum = netBalances.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(0L, sum, "Net balances must sum strictly to 0");

        List<SimplifiedDebtResponse> simplified = debtSimplificationEngine.simplifyDebts(netBalances, userNames);
        assertEquals(3, simplified.size());
        for (SimplifiedDebtResponse debt : simplified) {
            assertEquals(uAlice, debt.getToUserId());
            assertEquals(100000L, debt.getAmountCents());
        }
    }
}
