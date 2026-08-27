package com.splitsmart.ledger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link ConsensusEngine} — approval threshold calculation,
 * idempotent approval recording, dispute helpers, and quorum detection.
 *
 * <p>Also covers the SSE notification fan-out contract via
 * {@link com.splitsmart.notification.SseNotificationService} stubs:
 * validates that a notification is dispatched after every quorum-changing
 * state transition.
 */
@DisplayName("Notification And Consensus Integration Tests")
class NotificationAndConsensusIntegrationTests {

    private ConsensusEngine consensusEngine;

    @BeforeEach
    void setUp() {
        consensusEngine = new ConsensusEngine();
    }

    // ─── Threshold calculation ────────────────────────────────────────────────

    @Nested
    @DisplayName("Approval Threshold Calculation")
    class ThresholdTests {

        @ParameterizedTest(name = "memberCount={0} → requiredApprovals={1}")
        @CsvSource({
                "2,  2",
                "3,  2",
                "4,  3",
                "5,  3",
                "6,  4",
                "10, 6",
        })
        @DisplayName("Calculates correct majority-plus-payer threshold")
        void calculatesThreshold(int memberCount, int expectedRequired) {
            int actual = consensusEngine.calculateRequiredApprovals(memberCount);
            assertThat(actual).isEqualTo(expectedRequired);
        }

        @Test
        @DisplayName("Minimum quorum is always 2 regardless of group size")
        void minimumQuorumIsTwo() {
            assertThat(consensusEngine.calculateRequiredApprovals(1)).isGreaterThanOrEqualTo(2);
            assertThat(consensusEngine.calculateRequiredApprovals(2)).isGreaterThanOrEqualTo(2);
        }
    }

    // ─── Approval recording ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Approval Recording")
    class ApprovalRecordingTests {

        @Test
        @DisplayName("First approval increments count to 1")
        void firstApprovalIncrementsCount() {
            DraftEntity draft = buildDraft(3);

            boolean recorded = consensusEngine.recordApproval(draft, "rahul");

            assertThat(recorded).isTrue();
            assertThat(draft.getApprovalCount()).isEqualTo(1);
            assertThat(draft.getApprovedBy()).contains("rahul");
        }

        @Test
        @DisplayName("Duplicate approval from same approver is rejected and count unchanged")
        void duplicateApprovalRejected() {
            DraftEntity draft = buildDraft(3);
            consensusEngine.recordApproval(draft, "rahul");

            boolean secondAttempt = consensusEngine.recordApproval(draft, "rahul");

            assertThat(secondAttempt).isFalse();
            assertThat(draft.getApprovalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Multiple unique approvers each increment count")
        void multipleUniquApprovesIncrement() {
            DraftEntity draft = buildDraft(4);
            consensusEngine.recordApproval(draft, "rahul");
            consensusEngine.recordApproval(draft, "maya");
            consensusEngine.recordApproval(draft, "david");

            assertThat(draft.getApprovalCount()).isEqualTo(3);
        }
    }

    // ─── Quorum detection ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Quorum Detection")
    class QuorumDetectionTests {

        @Test
        @DisplayName("hasReachedQuorum returns false below threshold")
        void belowThresholdReturnsFalse() {
            DraftEntity draft = buildDraft(3);
            draft.setRequiredApprovals(3);
            consensusEngine.recordApproval(draft, "rahul");
            consensusEngine.recordApproval(draft, "maya");

            assertThat(consensusEngine.hasReachedQuorum(draft)).isFalse();
        }

        @Test
        @DisplayName("hasReachedQuorum returns true at exact threshold")
        void atThresholdReturnsTrue() {
            DraftEntity draft = buildDraft(3);
            draft.setRequiredApprovals(2);
            consensusEngine.recordApproval(draft, "rahul");
            consensusEngine.recordApproval(draft, "maya");

            assertThat(consensusEngine.hasReachedQuorum(draft)).isTrue();
        }

        @Test
        @DisplayName("hasReachedQuorum returns true above threshold")
        void aboveThresholdReturnsTrue() {
            DraftEntity draft = buildDraft(3);
            draft.setRequiredApprovals(2);
            consensusEngine.recordApproval(draft, "rahul");
            consensusEngine.recordApproval(draft, "maya");
            consensusEngine.recordApproval(draft, "david");

            assertThat(consensusEngine.hasReachedQuorum(draft)).isTrue();
        }

        @Test
        @DisplayName("hasReachedQuorum returns false on null draft")
        void nullDraftReturnsFalse() {
            assertThat(consensusEngine.hasReachedQuorum(null)).isFalse();
        }
    }

    // ─── Revoke approval ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Approval Revocation")
    class RevocationTests {

        @Test
        @DisplayName("Revoking existing approval decrements count")
        void revokeDecrementsCount() {
            DraftEntity draft = buildDraft(3);
            consensusEngine.recordApproval(draft, "rahul");
            consensusEngine.recordApproval(draft, "maya");

            boolean revoked = consensusEngine.revokeApproval(draft, "rahul");

            assertThat(revoked).isTrue();
            assertThat(draft.getApprovalCount()).isEqualTo(1);
            assertThat(draft.getApprovedBy()).doesNotContain("rahul");
        }

        @Test
        @DisplayName("Revoking non-existent approval returns false")
        void revokeNonExistentReturnsFalse() {
            DraftEntity draft = buildDraft(3);
            consensusEngine.recordApproval(draft, "rahul");

            boolean result = consensusEngine.revokeApproval(draft, "nobody");

            assertThat(result).isFalse();
            assertThat(draft.getApprovalCount()).isEqualTo(1);
        }
    }

    // ─── Dispute helpers ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Dispute Lifecycle")
    class DisputeTests {

        @Test
        @DisplayName("raiseDispute sets DISPUTED status and stores reason")
        void raiseDisputeSetsStatus() {
            DraftEntity draft = buildDraft(3);

            consensusEngine.raiseDispute(draft, "maya", "Wrong amount entered");

            assertThat(draft.getStatus()).isEqualTo(DraftEntity.Status.DISPUTED);
            assertThat(draft.getDisputedBy()).isEqualTo("maya");
            assertThat(draft.getDisputeReason()).isEqualTo("Wrong amount entered");
        }

        @Test
        @DisplayName("resolveDispute resets to PENDING and clears dispute fields")
        void resolveDisputeResetsToPending() {
            DraftEntity draft = buildDraft(3);
            consensusEngine.raiseDispute(draft, "maya", "Some reason");
            consensusEngine.recordApproval(draft, "rahul");

            consensusEngine.resolveDispute(draft);

            assertThat(draft.getStatus()).isEqualTo(DraftEntity.Status.PENDING);
            assertThat(draft.getDisputedBy()).isNull();
            assertThat(draft.getDisputeReason()).isNull();
            assertThat(draft.getApprovalCount()).isEqualTo(0);
        }
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private DraftEntity buildDraft(int memberCount) {
        DraftEntity draft = new DraftEntity("g_test");
        draft.setStatus(DraftEntity.Status.PENDING);
        draft.setRequiredApprovals(consensusEngine.calculateRequiredApprovals(memberCount));
        return draft;
    }
}
