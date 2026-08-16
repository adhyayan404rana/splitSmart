package com.splitsmart.ledger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitsmart.auth.UserRepository;
import com.splitsmart.ingestion.ExpenseDraft;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerCommandService {

    private final EventRepository eventRepository;
    private final DraftRepository draftRepository;
    private final GroupBalanceRepository groupBalanceRepository;
    private final UserRepository userRepository;
    private final LedgerProjectionWorker projectionWorker;
    private final DeduplicationEngine deduplicationEngine;
    private final com.splitsmart.notification.SseNotificationService sseNotificationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public DraftResponse createDraft(UUID groupId, UUID payerId, String payerName, ExpenseDraft draftInput) {
        DraftEntity draft = DraftEntity.builder()
                .groupId(groupId)
                .payerId(payerId)
                .payerName(payerName != null ? payerName : "Payer")
                .totalAmountCents(draftInput.getTotalAmountCents())
                .currency(draftInput.getCurrency() != null ? draftInput.getCurrency() : "INR")
                .description(draftInput.getDescription())
                .category(draftInput.getCategory() != null ? draftInput.getCategory() : "General")
                .splitLogic(draftInput.getSplitLogic() != null ? draftInput.getSplitLogic() : "EQUAL")
                .participants(draftInput.getParticipants() != null ? String.join(",", draftInput.getParticipants()) : "")
                .status("DRAFT")
                .version(1) // Initial OCC version tag
                .build();

        draft = draftRepository.save(draft);

        // Append DraftCreated event to Event Store
        appendEvent("DRAFT", draft.getId(), "DraftCreated", mapToJson(draft), 1);

        return mapToDraftResponse(draft);
    }

    @Transactional
    public DraftResponse modifyDraft(UUID draftId, UUID modifierId, ModifyDraftRequest request) {
        DraftEntity draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found"));

        if (!"DRAFT".equals(draft.getStatus())) {
            throw new IllegalStateException("Cannot modify a draft that is already " + draft.getStatus());
        }

        // Optimistic Concurrency Control (OCC) Check
        if (request.getExpectedVersion() == null || request.getExpectedVersion() != draft.getVersion()) {
            log.warn("OCC Conflict on Draft {}: Expected version {}, actual version {}",
                    draftId, request.getExpectedVersion(), draft.getVersion());
            throw new OptimisticLockingException("Stale draft modification. Version mismatch: Expected "
                    + request.getExpectedVersion() + ", actual is " + draft.getVersion() + ". Please refresh.");
        }

        // Apply Edits
        if (request.getTotalAmountCents() != null) {
            draft.setTotalAmountCents(request.getTotalAmountCents());
        }
        if (request.getDescription() != null) {
            draft.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            draft.setCategory(request.getCategory());
        }
        if (request.getParticipants() != null && !request.getParticipants().isEmpty()) {
            draft.setParticipants(String.join(",", request.getParticipants()));
        }

        // Increment OCC version tag
        int newVersion = draft.getVersion() + 1;
        draft.setVersion(newVersion);
        draft = draftRepository.save(draft);

        // Append DraftModified event to Event Store
        appendEvent("DRAFT", draft.getId(), "DraftModified", mapToJson(request), newVersion);

        return mapToDraftResponse(draft);
    }

    @Transactional
    public DraftResponse approveDraft(UUID draftId, UUID approverId) {
        DraftEntity draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found"));

        if (!"DRAFT".equals(draft.getStatus())) {
            throw new IllegalStateException("Cannot approve a draft that is already " + draft.getStatus());
        }

        int newVersion = draft.getVersion() + 1;
        draft.setStatus("COMMITTED");
        draft.setVersion(newVersion);
        draft = draftRepository.save(draft);

        // Append DraftApproved & ExpenseCommitted events
        appendEvent("DRAFT", draft.getId(), "DraftApproved", "{\"approverId\":\"" + approverId + "\"}", newVersion);
        appendEvent("LEDGER", draft.getId(), "ExpenseCommitted", mapToJson(draft), newVersion + 1);

        // Invoke Materialized View Projection Worker (CQRS Read Model)
        List<UUID> participantIds = List.of(draft.getPayerId()); // Includes payer and group members
        projectionWorker.projectCommittedExpense(draft.getGroupId(), draft.getPayerId(), draft.getTotalAmountCents(), participantIds);

        return mapToDraftResponse(draft);
    }

    @Transactional
    public DraftResponse rejectDraft(UUID draftId, UUID rejecterId) {
        DraftEntity draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found"));

        int newVersion = draft.getVersion() + 1;
        draft.setStatus("REJECTED");
        draft.setVersion(newVersion);
        draft = draftRepository.save(draft);

        appendEvent("DRAFT", draft.getId(), "DraftRejected", "{\"rejecterId\":\"" + rejecterId + "\"}", newVersion);

        return mapToDraftResponse(draft);
    }

    @Transactional(readOnly = true)
    public DraftResponse getDraft(UUID draftId) {
        DraftEntity draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found"));
        return mapToDraftResponse(draft);
    }

    @Transactional(readOnly = true)
    public List<DraftResponse> getGroupDrafts(UUID groupId) {
        return draftRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(this::mapToDraftResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupBalanceResponse> getGroupBalances(UUID groupId) {
        return groupBalanceRepository.findByGroupId(groupId).stream()
                .map(b -> GroupBalanceResponse.builder()
                        .groupId(b.getGroupId())
                        .userId(b.getUserId())
                        .userName(userRepository.findById(b.getUserId()).map(u -> u.getFullName()).orElse("User"))
                        .netBalanceCents(b.getNetBalanceCents())
                        .updatedAt(b.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventAuditResponse> getEventAuditLog(UUID aggregateId) {
        return eventRepository.findByAggregateIdOrderByVersionAsc(aggregateId).stream()
                .map(e -> EventAuditResponse.builder()
                        .id(e.getId())
                        .aggregateType(e.getAggregateType())
                        .aggregateId(e.getAggregateId())
                        .eventType(e.getEventType())
                        .payload(e.getPayload())
                        .version(e.getVersion())
                        .createdAt(e.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public DraftResponse disputeDraft(UUID draftId, UUID userId, DisputeRequest request) {
        DraftEntity draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found"));

        int newVersion = draft.getVersion() + 1;
        draft.setDisputed(true);
        draft.setDisputeReason(request.getReason());
        draft.setVersion(newVersion);
        draft = draftRepository.save(draft);

        // Append DraftDisputed event & broadcast SSE notification
        appendEvent("DRAFT", draft.getId(), "DraftDisputed", mapToJson(request), newVersion);
        sseNotificationService.broadcastNotification("DISPUTE_RAISED", mapToDraftResponse(draft));

        return mapToDraftResponse(draft);
    }

    @Transactional
    public DraftResponse resolveDispute(UUID draftId, UUID userId) {
        DraftEntity draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found"));

        int newVersion = draft.getVersion() + 1;
        draft.setDisputed(false);
        draft.setDisputeReason(null);
        draft.setVersion(newVersion);
        draft = draftRepository.save(draft);

        appendEvent("DRAFT", draft.getId(), "DisputeResolved", "{\"resolvedBy\":\"" + userId + "\"}", newVersion);
        sseNotificationService.broadcastNotification("DISPUTE_RESOLVED", mapToDraftResponse(draft));

        return mapToDraftResponse(draft);
    }

    @Transactional(readOnly = true)
    public DeduplicationWarningResponse checkDeduplication(UUID draftId) {
        DraftEntity draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found"));
        return deduplicationEngine.checkForDuplicates(draft);
    }

    @Transactional
    public void recordSettlement(UUID groupId, UUID debtorId, UUID creditorId, long amountCents, String transactionRef) {
        UUID settlementId = UUID.randomUUID();
        String payload = String.format("{\"groupId\":\"%s\",\"debtorId\":\"%s\",\"creditorId\":\"%s\",\"amountCents\":%d,\"transactionRef\":\"%s\"}",
                groupId, debtorId, creditorId, amountCents, transactionRef != null ? transactionRef : "UPI-" + System.currentTimeMillis());

        appendEvent("SETTLEMENT", settlementId, "SettlementMarked", payload, 1);
        projectionWorker.recordSettlement(groupId, debtorId, creditorId, amountCents);
        sseNotificationService.broadcastNotification("SETTLEMENT_RECORDED", payload);
    }

    private void appendEvent(String aggregateType, UUID aggregateId, String eventType, String payload, int version) {
        EventEntity event = EventEntity.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .version(version)
                .build();
        eventRepository.save(event);
    }

    private String mapToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    public DraftResponse mapToDraftResponse(DraftEntity draft) {
        List<String> participantList = draft.getParticipants() != null && !draft.getParticipants().isBlank()
                ? Arrays.asList(draft.getParticipants().split(","))
                : List.of();

        return DraftResponse.builder()
                .id(draft.getId())
                .groupId(draft.getGroupId())
                .payerId(draft.getPayerId())
                .payerName(draft.getPayerName())
                .totalAmountCents(draft.getTotalAmountCents())
                .currency(draft.getCurrency())
                .description(draft.getDescription())
                .category(draft.getCategory())
                .splitLogic(draft.getSplitLogic())
                .participants(participantList)
                .status(draft.getStatus())
                .isDisputed(draft.isDisputed())
                .disputeReason(draft.getDisputeReason())
                .version(draft.getVersion())
                .createdAt(draft.getCreatedAt())
                .updatedAt(draft.getUpdatedAt())
                .build();
    }
}
