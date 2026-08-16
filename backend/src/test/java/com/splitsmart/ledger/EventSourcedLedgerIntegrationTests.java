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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:ledgerdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class EventSourcedLedgerIntegrationTests {

    @Autowired
    private LedgerCommandService ledgerCommandService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private GroupBalanceRepository groupBalanceRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID groupId;
    private UUID payerId;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        UserEntity payer = userRepository.save(UserEntity.builder()
                .email("sarah_ledger@example.com")
                .fullName("Sarah Payer")
                .passwordHash("hashedpass")
                .build());
        payerId = payer.getId();
    }

    @Test
    void testCreateDraftAndEventStoreAppend() {
        ExpenseDraft input = ExpenseDraft.builder()
                .totalAmountCents(400000L) // 4000.00 INR
                .currency("INR")
                .description("Dinner at Shacks")
                .category("Food & Dining")
                .participants(List.of("Sarah", "Rahul", "Amit"))
                .build();

        DraftResponse draft = ledgerCommandService.createDraft(groupId, payerId, "Sarah Payer", input);

        assertNotNull(draft.getId());
        assertEquals("DRAFT", draft.getStatus());
        assertEquals(1, draft.getVersion());

        // Verify DraftCreated event appended to Event Store
        List<EventAuditResponse> auditLog = ledgerCommandService.getEventAuditLog(draft.getId());
        assertEquals(1, auditLog.size());
        assertEquals("DraftCreated", auditLog.get(0).getEventType());
        assertEquals(1, auditLog.get(0).getVersion());
    }

    @Test
    void testOptimisticConcurrencyControlConflict() {
        ExpenseDraft input = ExpenseDraft.builder()
                .totalAmountCents(150000L)
                .description("Drinks")
                .build();

        DraftResponse draft = ledgerCommandService.createDraft(groupId, payerId, "Sarah Payer", input);

        // Edit 1 with correct version = 1 -> succeeds, version becomes 2
        ModifyDraftRequest validReq = new ModifyDraftRequest();
        validReq.setExpectedVersion(1);
        validReq.setTotalAmountCents(180000L); // Modified amount

        DraftResponse updatedDraft = ledgerCommandService.modifyDraft(draft.getId(), payerId, validReq);
        assertEquals(2, updatedDraft.getVersion());
        assertEquals(180000L, updatedDraft.getTotalAmountCents());

        // Edit 2 with stale version = 1 (simulating concurrent user edit) -> throws OptimisticLockingException
        ModifyDraftRequest staleReq = new ModifyDraftRequest();
        staleReq.setExpectedVersion(1); // Stale expected version!
        staleReq.setTotalAmountCents(200000L);

        assertThrows(OptimisticLockingException.class, () -> {
            ledgerCommandService.modifyDraft(draft.getId(), payerId, staleReq);
        });
    }

    @Test
    void testApproveDraftAndMaterializedBalanceProjection() {
        ExpenseDraft input = ExpenseDraft.builder()
                .totalAmountCents(300000L) // 3000.00 INR
                .description("Airbnb")
                .build();

        DraftResponse draft = ledgerCommandService.createDraft(groupId, payerId, "Sarah Payer", input);

        // Approve draft
        DraftResponse approvedDraft = ledgerCommandService.approveDraft(draft.getId(), payerId);
        assertEquals("COMMITTED", approvedDraft.getStatus());

        // Verify Event Audit Log contains DraftCreated, DraftApproved, and ExpenseCommitted events
        List<EventAuditResponse> auditLog = ledgerCommandService.getEventAuditLog(draft.getId());
        assertTrue(auditLog.stream().anyMatch(e -> "ExpenseCommitted".equals(e.getEventType())));

        // Verify Materialized Read View group_balances_view updated
        List<GroupBalanceResponse> balances = ledgerCommandService.getGroupBalances(groupId);
        assertFalse(balances.isEmpty());
        assertTrue(balances.stream().anyMatch(b -> b.getUserId().equals(payerId)));
    }
}
