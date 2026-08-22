package com.splitsmart.ledger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CQRS write-path service for the SplitSmart ledger.
 *
 * <p>All mutating operations (create draft, approve, modify, dispute, finalize)
 * go through this service. Each operation:
 * <ol>
 *   <li>Validates preconditions.</li>
 *   <li>Runs deduplication if applicable.</li>
 *   <li>Mutates the {@link DraftEntity}.</li>
 *   <li>Appends an immutable {@link EventEntity} to the event store using
 *       OCC version locking.</li>
 *   <li>Persists both via JPA within a single transaction.</li>
 * </ol>
 *
 * <p>Read operations are served by the projection layer
 * ({@code LedgerProjectionWorker} and {@code GroupBalanceRepository}).
 *
 * <p>Uses the Fowler Money pattern: all monetary values are stored as
 * {@code long} minor units (paise) in the entity layer and converted to
 * {@link BigDecimal} major units only in the DTO layer.
 */
@Service
public class LedgerCommandService {

    private static final Logger log = LoggerFactory.getLogger(LedgerCommandService.class);

    private final DraftRepository       draftRepository;
    private final EventRepository       eventRepository;
    private final ConsensusEngine       consensusEngine;
    private final DeduplicationEngine   deduplicationEngine;
    private final ObjectMapper          objectMapper;

    public LedgerCommandService(DraftRepository draftRepository,
                                EventRepository eventRepository,
                                ConsensusEngine consensusEngine,
                                DeduplicationEngine deduplicationEngine,
                                ObjectMapper objectMapper) {
        this.draftRepository    = draftRepository;
        this.eventRepository    = eventRepository;
        this.consensusEngine    = consensusEngine;
        this.deduplicationEngine = deduplicationEngine;
        this.objectMapper       = objectMapper;
    }

    // ─── Create draft ────────────────────────────────────────────────────────

    /**
     * Creates a new expense draft after deduplication check.
     *
     * @return the persisted {@link DraftEntity}
     * @throws IllegalStateException if a duplicate is detected (exact match)
     */
    @Transactional
    public DraftEntity createDraft(String groupId, String description, long amountMinor,
                                   String currency, String payerIdentifier, String splitType,
                                   String category, LocalDate transactionDate,
                                   List<String> participants, String correlationId,
                                   String actorId, String extractionSource, int tier, int confidence) {

        // ── Deduplication ────────────────────────────────────────────────────
        var dedup = deduplicationEngine.check(groupId, payerIdentifier, amountMinor, currency,
                description, transactionDate != null ? transactionDate.toString() : "");
        if (dedup.getKind() == DeduplicationEngine.DeduplicationResult.Kind.EXACT) {
            throw new IllegalStateException("Duplicate draft detected: " + dedup.getExistingDraftId());
        }

        // ── Build entity ─────────────────────────────────────────────────────
        DraftEntity draft = new DraftEntity(groupId);
        draft.setDescription(description);
        draft.setAmountMinor(amountMinor);
        draft.setCurrency(currency != null ? currency : "INR");
        draft.setPayerIdentifier(payerIdentifier);
        draft.setSplitType(splitType != null ? splitType : "EQUAL");
        draft.setCategory(category != null ? category : "Bills");
        draft.setTransactionDate(transactionDate);
        draft.setParticipants(participants != null ? String.join(",", participants) : "");
        draft.setCorrelationId(correlationId);
        draft.setExtractionSource(extractionSource);
        draft.setExtractionTier(tier);
        draft.setConfidence(confidence);
        draft.setRequiredApprovals(consensusEngine.calculateRequiredApprovals(
                participants != null ? participants.size() + 1 : 2));
        draft.setExpiresAt(Instant.now().plusSeconds(86_400)); // 24-hour TTL

        draft = draftRepository.save(draft);

        // ── Append event ─────────────────────────────────────────────────────
        appendEvent(groupId, "DraftCreated", actorId, correlationId,
                Map.of("draftId", draft.getId(), "description", description,
                       "amount", amountMinor, "currency", draft.getCurrency()));

        // ── Register fingerprint ─────────────────────────────────────────────
        deduplicationEngine.register(groupId, dedup.getFingerprint(), draft.getId());

        log.info("[LedgerCommandService] Draft created — id={} groupId={}", draft.getId(), groupId);
        return draft;
    }

    // ─── Approve draft ───────────────────────────────────────────────────────

    /**
     * Records an approval from {@code approverId} on the specified draft.
     * Finalizes the draft if quorum is reached.
     *
     * @return updated {@link DraftEntity}
     */
    @Transactional
    public DraftEntity approveDraft(String draftId, String approverId) {
        DraftEntity draft = findDraftOrThrow(draftId);
        assertStatus(draft, DraftEntity.Status.PENDING, "approve");

        boolean recorded = consensusEngine.recordApproval(draft, approverId);
        if (!recorded) return draft; // idempotent

        if (consensusEngine.hasReachedQuorum(draft)) {
            draft.setStatus(DraftEntity.Status.APPROVED);
            appendEvent(draft.getGroupId(), "DraftApproved", approverId, null,
                    Map.of("draftId", draftId, "approvalCount", draft.getApprovalCount()));
        } else {
            appendEvent(draft.getGroupId(), "DraftApprovalRecorded", approverId, null,
                    Map.of("draftId", draftId, "approvalCount", draft.getApprovalCount(),
                           "remaining", draft.getRequiredApprovals() - draft.getApprovalCount()));
        }

        return draftRepository.save(draft);
    }

    // ─── Revoke approval ─────────────────────────────────────────────────────

    /**
     * Revokes a previously recorded approval.
     */
    @Transactional
    public DraftEntity revokeApproval(String draftId, String approverId) {
        DraftEntity draft = findDraftOrThrow(draftId);
        assertStatus(draft, DraftEntity.Status.PENDING, "revoke approval");

        consensusEngine.revokeApproval(draft, approverId);
        appendEvent(draft.getGroupId(), "ApprovalRevoked", approverId, null,
                Map.of("draftId", draftId, "revokedBy", approverId));

        return draftRepository.save(draft);
    }

    // ─── Modify draft ────────────────────────────────────────────────────────

    /**
     * Applies a partial modification to a draft and resets approvals.
     */
    @Transactional
    public DraftEntity modifyDraft(String draftId, ModifyDraftRequest req, String actorId) {
        DraftEntity draft = findDraftOrThrow(draftId);
        if (draft.getStatus() == DraftEntity.Status.FINALIZED) {
            throw new IllegalStateException("Cannot modify a finalized draft");
        }

        if (req.getDescription()     != null) draft.setDescription(req.getDescription());
        if (req.getAmount()          != null) draft.setAmountMinor(req.getAmount().movePointRight(2).longValue());
        if (req.getCurrency()        != null) draft.setCurrency(req.getCurrency());
        if (req.getPayerIdentifier() != null) draft.setPayerIdentifier(req.getPayerIdentifier());
        if (req.getSplitType()       != null) draft.setSplitType(req.getSplitType());
        if (req.getCategory()        != null) draft.setCategory(req.getCategory());
        if (req.getTransactionDate() != null) draft.setTransactionDate(req.getTransactionDate());
        if (req.getParticipants()    != null) draft.setParticipants(String.join(",", req.getParticipants()));

        // Reset consensus on modification
        draft.setApprovedBy(null);
        draft.setApprovalCount(0);
        draft.setStatus(DraftEntity.Status.PENDING);

        appendEvent(draft.getGroupId(), "DraftModified", actorId, null,
                Map.of("draftId", draftId, "modifiedBy", actorId,
                       "reason", req.getModificationReason() != null ? req.getModificationReason() : ""));

        log.info("[LedgerCommandService] Draft modified and approvals reset — id={}", draftId);
        return draftRepository.save(draft);
    }

    // ─── Dispute draft ───────────────────────────────────────────────────────

    /**
     * Raises a dispute on a draft.
     */
    @Transactional
    public DraftEntity disputeDraft(String draftId, DisputeRequest req) {
        DraftEntity draft = findDraftOrThrow(draftId);
        assertStatus(draft, DraftEntity.Status.PENDING, "dispute");

        consensusEngine.raiseDispute(draft, req.getDisputedBy(), req.getReason());
        appendEvent(draft.getGroupId(), "DraftDisputed", req.getDisputedBy(), null,
                Map.of("draftId", draftId, "reason", req.getReason()));

        return draftRepository.save(draft);
    }

    // ─── Resolve dispute ─────────────────────────────────────────────────────

    /**
     * Resolves an active dispute, resetting the draft to PENDING.
     */
    @Transactional
    public DraftEntity resolveDispute(String draftId, String actorId) {
        DraftEntity draft = findDraftOrThrow(draftId);
        if (draft.getStatus() != DraftEntity.Status.DISPUTED) {
            throw new IllegalStateException("Draft is not in DISPUTED state");
        }

        consensusEngine.resolveDispute(draft);
        appendEvent(draft.getGroupId(), "ConflictResolved", actorId, null,
                Map.of("draftId", draftId, "resolvedBy", actorId));

        return draftRepository.save(draft);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private DraftEntity findDraftOrThrow(String draftId) {
        return draftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found: " + draftId));
    }

    private void assertStatus(DraftEntity draft, DraftEntity.Status expected, String operation) {
        if (draft.getStatus() != expected) {
            throw new IllegalStateException(String.format(
                    "Cannot %s draft %s in status %s (expected %s)",
                    operation, draft.getId(), draft.getStatus(), expected));
        }
    }

    /**
     * Appends an event to the group's event stream using OCC version locking.
     * Retries once on version conflict before propagating the exception.
     */
    private void appendEvent(String groupId, String eventType, String actorId,
                              String correlationId, Map<String, Object> payloadMap) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                long nextVersion = eventRepository.findMaxVersionByGroupId(groupId)
                        .map(v -> v + 1).orElse(1L);
                String payload = toJson(payloadMap);

                EventEntity event = EventEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .groupId(groupId)
                        .version(nextVersion)
                        .eventType(eventType)
                        .actorId(actorId)
                        .correlationId(correlationId)
                        .payload(payload)
                        .build();

                eventRepository.save(event);
                log.debug("[LedgerCommandService] Event appended — type={} version={} groupId={}",
                        eventType, nextVersion, groupId);
                return;

            } catch (DataIntegrityViolationException e) {
                if (attempt == 2) {
                    throw new OptimisticLockingException(groupId, -1, -1);
                }
                log.warn("[LedgerCommandService] OCC conflict on attempt {} for groupId={} — retrying", attempt, groupId);
            }
        }
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
