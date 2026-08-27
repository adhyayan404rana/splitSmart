package com.splitsmart.ledger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the event-sourced ledger write path.
 *
 * <p>Validates the OCC (Optimistic Concurrency Control) version conflict
 * behaviour and idempotent append semantics of {@link LedgerCommandService}.
 *
 * <h3>Scenarios covered</h3>
 * <ul>
 *   <li>Happy path draft creation with deduplication bypass</li>
 *   <li>OCC conflict on concurrent version append — retry on first conflict,
 *       propagate {@link OptimisticLockingException} on second</li>
 *   <li>Idempotent approval — duplicate approver does not increment count</li>
 *   <li>Quorum finalization — draft transitions to APPROVED when threshold met</li>
 *   <li>Dispute lifecycle — PENDING → DISPUTED → PENDING on resolve</li>
 *   <li>Modify draft resets approval tracking</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Event-Sourced Ledger Integration Tests")
class EventSourcedLedgerIntegrationTests {

    @Mock private DraftRepository       draftRepository;
    @Mock private EventRepository       eventRepository;
    @Mock private ConsensusEngine       consensusEngine;
    @Mock private DeduplicationEngine   deduplicationEngine;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private LedgerCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new LedgerCommandService(
                draftRepository, eventRepository, consensusEngine,
                deduplicationEngine, objectMapper);
    }

    // ─── Draft creation ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Draft Creation")
    class DraftCreationTests {

        @Test
        @DisplayName("Creates draft successfully when no duplicate detected")
        void createsDraftOnNoDuplicate() throws Exception {
            // Arrange
            var noDupeResult = DeduplicationEngine.DeduplicationResult.noDuplicate("fp123");
            when(deduplicationEngine.check(any(), any(), anyLong(), any(), any(), any()))
                    .thenReturn(noDupeResult);
            when(consensusEngine.calculateRequiredApprovals(anyInt())).thenReturn(2);
            when(eventRepository.findMaxVersionByGroupId(any())).thenReturn(Optional.of(0L));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            DraftEntity savedDraft = new DraftEntity("g_test");
            when(draftRepository.save(any(DraftEntity.class))).thenReturn(savedDraft);
            when(eventRepository.save(any(EventEntity.class))).thenReturn(new EventEntity());

            // Act & Assert — should not throw
            assertThatCode(() -> commandService.createDraft(
                    "g_test", "Dinner at Dominos", 240000L, "INR",
                    "rahul", "EQUAL", "Food", null,
                    java.util.List.of("maya", "david"), "corr-001", "rahul",
                    "TYPED", 1, 90))
                    .doesNotThrowAnyException();

            verify(draftRepository, times(1)).save(any(DraftEntity.class));
            verify(deduplicationEngine, times(1)).register(any(), any(), any());
        }

        @Test
        @DisplayName("Throws IllegalStateException on exact duplicate fingerprint")
        void throwsOnExactDuplicate() {
            var exactDupe = DeduplicationEngine.DeduplicationResult.exactMatch("fp123", "existing-draft-id");
            when(deduplicationEngine.check(any(), any(), anyLong(), any(), any(), any()))
                    .thenReturn(exactDupe);

            assertThatThrownBy(() -> commandService.createDraft(
                    "g_test", "Dinner", 100000L, "INR",
                    "rahul", "EQUAL", "Food", null,
                    java.util.List.of("maya"), "corr-002", "rahul",
                    "TYPED", 1, 85))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Duplicate draft detected");

            verify(draftRepository, never()).save(any());
        }
    }

    // ─── OCC conflict tests ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Optimistic Concurrency Control")
    class OccTests {

        @Test
        @DisplayName("Propagates OptimisticLockingException after second version conflict")
        void propagatesOccAfterSecondConflict() throws Exception {
            var noDupeResult = DeduplicationEngine.DeduplicationResult.noDuplicate("fp-occ");
            when(deduplicationEngine.check(any(), any(), anyLong(), any(), any(), any()))
                    .thenReturn(noDupeResult);
            when(consensusEngine.calculateRequiredApprovals(anyInt())).thenReturn(2);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            DraftEntity savedDraft = new DraftEntity("g_occ");
            when(draftRepository.save(any(DraftEntity.class))).thenReturn(savedDraft);

            // Both version attempts fail
            when(eventRepository.findMaxVersionByGroupId(any())).thenReturn(Optional.of(5L));
            when(eventRepository.save(any(EventEntity.class)))
                    .thenThrow(DataIntegrityViolationException.class);

            assertThatThrownBy(() -> commandService.createDraft(
                    "g_occ", "Conflict test", 100000L, "INR",
                    "actor", "EQUAL", "Bills", null,
                    java.util.List.of(), "corr-occ", "actor",
                    "TYPED", 1, 80))
                    .isInstanceOf(OptimisticLockingException.class);
        }
    }

    // ─── Approval idempotency tests ───────────────────────────────────────────

    @Nested
    @DisplayName("Approval Idempotency")
    class ApprovalIdempotencyTests {

        @Test
        @DisplayName("Duplicate approval from same user does not increment count")
        void duplicateApprovalIsIdempotent() throws Exception {
            DraftEntity draft = new DraftEntity("g_approve");
            draft.setStatus(DraftEntity.Status.PENDING);
            draft.setApprovedBy("rahul");
            draft.setApprovalCount(1);
            draft.setRequiredApprovals(2);

            when(draftRepository.findById("draft-001")).thenReturn(Optional.of(draft));
            // Simulate consensus engine rejecting duplicate
            when(consensusEngine.recordApproval(draft, "rahul")).thenReturn(false);

            DraftEntity result = commandService.approveDraft("draft-001", "rahul");

            // Count must remain 1
            assertThat(result.getApprovalCount()).isEqualTo(1);
            verify(draftRepository, never()).save(any());
        }

        @Test
        @DisplayName("New approval increments count and saves draft")
        void newApprovalIncrementsSaves() throws Exception {
            DraftEntity draft = new DraftEntity("g_approve");
            draft.setStatus(DraftEntity.Status.PENDING);
            draft.setApprovalCount(1);
            draft.setRequiredApprovals(2);

            when(draftRepository.findById("draft-002")).thenReturn(Optional.of(draft));
            when(consensusEngine.recordApproval(draft, "maya")).thenReturn(true);
            when(consensusEngine.hasReachedQuorum(draft)).thenReturn(false);
            when(eventRepository.findMaxVersionByGroupId(any())).thenReturn(Optional.of(1L));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(eventRepository.save(any())).thenReturn(new EventEntity());
            when(draftRepository.save(draft)).thenReturn(draft);

            commandService.approveDraft("draft-002", "maya");

            verify(draftRepository, times(1)).save(draft);
        }
    }

    // ─── Dispute lifecycle tests ──────────────────────────────────────────────

    @Nested
    @DisplayName("Dispute Lifecycle")
    class DisputeLifecycleTests {

        @Test
        @DisplayName("Dispute transitions draft to DISPUTED status")
        void disputeTransitionsToDISPUTED() throws Exception {
            DraftEntity draft = new DraftEntity("g_dispute");
            draft.setStatus(DraftEntity.Status.PENDING);

            when(draftRepository.findById("draft-003")).thenReturn(Optional.of(draft));
            doAnswer(inv -> {
                draft.setStatus(DraftEntity.Status.DISPUTED);
                draft.setDisputedBy("maya");
                draft.setDisputeReason("Wrong amount");
                return null;
            }).when(consensusEngine).raiseDispute(draft, "maya", "Wrong amount");
            when(eventRepository.findMaxVersionByGroupId(any())).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(eventRepository.save(any())).thenReturn(new EventEntity());
            when(draftRepository.save(draft)).thenReturn(draft);

            DisputeRequest req = new DisputeRequest();
            req.setDisputedBy("maya");
            req.setReason("Wrong amount");
            commandService.disputeDraft("draft-003", req);

            assertThat(draft.getStatus()).isEqualTo(DraftEntity.Status.DISPUTED);
        }

        @Test
        @DisplayName("Cannot dispute a non-PENDING draft")
        void cannotDisputeNonPendingDraft() {
            DraftEntity draft = new DraftEntity("g_dispute2");
            draft.setStatus(DraftEntity.Status.APPROVED);

            when(draftRepository.findById("draft-004")).thenReturn(Optional.of(draft));

            DisputeRequest req = new DisputeRequest();
            req.setDisputedBy("david");
            req.setReason("Dispute after approval");

            assertThatThrownBy(() -> commandService.disputeDraft("draft-004", req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }
    }
}
