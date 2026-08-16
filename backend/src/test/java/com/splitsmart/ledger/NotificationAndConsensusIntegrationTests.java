package com.splitsmart.ledger;

import com.splitsmart.auth.UserEntity;
import com.splitsmart.auth.UserRepository;
import com.splitsmart.ingestion.ExpenseDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:consensusdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class NotificationAndConsensusIntegrationTests {

    @Autowired
    private LedgerCommandService ledgerCommandService;

    @Autowired
    private ConsensusEngine consensusEngine;

    @Autowired
    private UserRepository userRepository;

    private UUID groupId;
    private UUID payerId;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        UserEntity payer = userRepository.save(UserEntity.builder()
                .email("sarah_consensus@example.com")
                .fullName("Sarah Payer")
                .passwordHash("hashedpass")
                .build());
        payerId = payer.getId();
    }

    @Test
    void testConsensusEvaluationPayerAndDebtorApproved() {
        // FR-2 Consensus Rule: Draft is committed ONLY after Payer + at least one Debtor approve
        ConsensusStatusResponse res = consensusEngine.evaluateConsensus(
                true, // Payer approved
                1,    // 1 Debtor approved
                2,    // 2 total Debtors
                Map.of("sarah", "APPROVED", "rahul", "APPROVED", "amit", "PENDING")
        );

        assertTrue(res.isConsensusReached(), "Consensus must be reached when Payer + 1 Debtor approve");
    }

    @Test
    void testDisputeFlaggingAndFrozenSettlement() {
        ExpenseDraft input = ExpenseDraft.builder()
                .totalAmountCents(250000L) // 2500.00 INR
                .description("Taxi Fare")
                .build();

        DraftResponse draft = ledgerCommandService.createDraft(groupId, payerId, "Sarah Payer", input);

        // Flag expense with dispute
        DisputeRequest disputeReq = new DisputeRequest();
        disputeReq.setReason("Incorrect split ratio calculated. Amit did not take the taxi.");

        DraftResponse disputedDraft = ledgerCommandService.disputeDraft(draft.getId(), payerId, disputeReq);

        assertTrue(disputedDraft.isDisputed(), "Expense must be marked as disputed");
        assertEquals("Incorrect split ratio calculated. Amit did not take the taxi.", disputedDraft.getDisputeReason());

        // Resolve dispute
        DraftResponse resolvedDraft = ledgerCommandService.resolveDispute(draft.getId(), payerId);
        assertFalse(resolvedDraft.isDisputed());
        assertNull(resolvedDraft.getDisputeReason());
    }

    @Test
    void testDeduplicationDetectionWarning() {
        ExpenseDraft input1 = ExpenseDraft.builder()
                .totalAmountCents(400000L)
                .description("Dinner at shacks")
                .build();

        DraftResponse draft1 = ledgerCommandService.createDraft(groupId, payerId, "Sarah Payer", input1);

        ExpenseDraft input2 = ExpenseDraft.builder()
                .totalAmountCents(400000L) // Exact same amount and payer
                .description("Dinner at shacks duplicate")
                .build();

        DraftResponse draft2 = ledgerCommandService.createDraft(groupId, payerId, "Sarah Payer", input2);

        DeduplicationWarningResponse dedup = ledgerCommandService.checkDeduplication(draft2.getId());

        assertTrue(dedup.isDuplicateWarning(), "Duplicate warning must be triggered for similar amount/payer");
        assertTrue(dedup.getCandidateDuplicateDraftIds().contains(draft1.getId()));
    }
}
