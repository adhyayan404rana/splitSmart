package com.splitsmart.ledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * High-level API facade over the CQRS ledger write and read sides.
 *
 * <p>Controllers and external services interact exclusively with this class.
 * It delegates writes to {@link LedgerCommandService} and reads to the
 * projection repositories ({@link DraftRepository}, {@link EventRepository},
 * {@link GroupBalanceRepository}).
 *
 * <p>This layer is responsible for assembling response DTOs from raw entity
 * data so that the command service remains free of presentation concerns.
 */
@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final LedgerCommandService   commandService;
    private final DraftRepository        draftRepository;
    private final EventRepository        eventRepository;
    private final GroupBalanceRepository groupBalanceRepository;

    public LedgerService(LedgerCommandService commandService,
                         DraftRepository draftRepository,
                         EventRepository eventRepository,
                         GroupBalanceRepository groupBalanceRepository) {
        this.commandService         = commandService;
        this.draftRepository        = draftRepository;
        this.eventRepository        = eventRepository;
        this.groupBalanceRepository = groupBalanceRepository;
    }

    // ─── Draft reads ─────────────────────────────────────────────────────────

    /**
     * Returns all pending drafts for a group, mapped to response DTOs.
     */
    @Transactional(readOnly = true)
    public List<DraftResponse> getPendingDrafts(String groupId) {
        return draftRepository
                .findByGroupIdAndStatusOrderByCreatedAtDesc(groupId, DraftEntity.Status.PENDING)
                .stream()
                .map(DraftResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Returns all drafts for a group regardless of status.
     */
    @Transactional(readOnly = true)
    public List<DraftResponse> getAllDrafts(String groupId) {
        return draftRepository
                .findByGroupIdOrderByCreatedAtDesc(groupId)
                .stream()
                .map(DraftResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single draft by ID, or empty if not found.
     */
    @Transactional(readOnly = true)
    public Optional<DraftResponse> getDraft(String draftId) {
        return draftRepository.findById(draftId).map(DraftResponse::from);
    }

    /**
     * Returns the consensus status for a draft.
     */
    @Transactional(readOnly = true)
    public Optional<ConsensusStatusResponse> getConsensusStatus(String draftId) {
        return draftRepository.findById(draftId).map(draft -> {
            List<String> approvedBy = draft.getApprovedBy() != null && !draft.getApprovedBy().isBlank()
                    ? List.of(draft.getApprovedBy().split(","))
                    : List.of();
            List<String> participants = draft.getParticipants() != null && !draft.getParticipants().isBlank()
                    ? List.of(draft.getParticipants().split(","))
                    : List.of();
            List<String> pending = participants.stream()
                    .filter(p -> !approvedBy.contains(p))
                    .collect(Collectors.toList());

            return ConsensusStatusResponse.builder()
                    .draftId(draft.getId())
                    .groupId(draft.getGroupId())
                    .status(draft.getStatus().name())
                    .approvalCount(draft.getApprovalCount())
                    .requiredApprovals(draft.getRequiredApprovals())
                    .approvedBy(approvedBy)
                    .pendingApprovers(pending)
                    .isDisputed(draft.getStatus() == DraftEntity.Status.DISPUTED)
                    .disputedBy(draft.getDisputedBy())
                    .disputeReason(draft.getDisputeReason())
                    .build();
        });
    }

    // ─── Draft writes (delegated to command service) ──────────────────────────

    public DraftResponse approveDraft(String draftId, String approverId) {
        return DraftResponse.from(commandService.approveDraft(draftId, approverId));
    }

    public DraftResponse revokeApproval(String draftId, String approverId) {
        return DraftResponse.from(commandService.revokeApproval(draftId, approverId));
    }

    public DraftResponse modifyDraft(String draftId, ModifyDraftRequest req, String actorId) {
        return DraftResponse.from(commandService.modifyDraft(draftId, req, actorId));
    }

    public DraftResponse disputeDraft(String draftId, DisputeRequest req) {
        return DraftResponse.from(commandService.disputeDraft(draftId, req));
    }

    public DraftResponse resolveDispute(String draftId, String actorId) {
        return DraftResponse.from(commandService.resolveDispute(draftId, actorId));
    }

    // ─── Balance reads ────────────────────────────────────────────────────────

    /**
     * Returns the full group balance summary with per-member breakdown.
     */
    @Transactional(readOnly = true)
    public GroupBalanceResponse getGroupBalances(String groupId) {
        List<GroupBalanceEntity> entities =
                groupBalanceRepository.findByGroupIdOrderByNetBalanceDesc(groupId);

        BigDecimal totalSpend = groupBalanceRepository.sumTotalOwedByGroupId(groupId)
                .orElse(BigDecimal.ZERO);
        boolean fullySettled = groupBalanceRepository.isGroupFullySettled(groupId);
        String currency = entities.isEmpty() ? "INR" : entities.get(0).getCurrency();

        List<GroupBalanceResponse.MemberBalance> members = entities.stream()
                .map(GroupBalanceResponse.MemberBalance::from)
                .collect(Collectors.toList());

        return GroupBalanceResponse.builder()
                .groupId(groupId)
                .currency(currency)
                .totalSpend(totalSpend)
                .members(members)
                .fullySettled(fullySettled)
                .build();
    }

    // ─── Audit feed ──────────────────────────────────────────────────────────

    /**
     * Returns the full ordered audit event stream for a group.
     */
    @Transactional(readOnly = true)
    public List<EventAuditResponse> getAuditFeed(String groupId) {
        return eventRepository.findByGroupIdOrderByVersionAsc(groupId).stream()
                .map(e -> EventAuditResponse.from(e, e.getActorId()))
                .collect(Collectors.toList());
    }

    // ─── Pending draft count for badge ───────────────────────────────────────

    public long getPendingDraftCount(String groupId) {
        return draftRepository.countByGroupIdAndStatus(groupId, DraftEntity.Status.PENDING);
    }
}
