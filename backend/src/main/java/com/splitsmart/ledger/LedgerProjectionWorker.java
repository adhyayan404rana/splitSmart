package com.splitsmart.ledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * CQRS read-side projection worker.
 *
 * <p>Listens on an internal Spring application event (published by
 * {@link LedgerCommandService} after each successful event append) and
 * incrementally updates the {@link GroupBalanceEntity} materialized view.
 *
 * <p>The worker processes one event at a time per group to avoid race
 * conditions on the balance rows. It tracks the last processed version per
 * group via {@link GroupBalanceRepository#findMaxLastEventVersionByGroupId}
 * so it can resume from the correct position after a restart.
 *
 * <h3>Supported event types</h3>
 * <ul>
 *   <li>{@code DraftApproved} — credits the payer and debits all participants.</li>
 *   <li>{@code SettlementRecorded} — offsets the payer's credit against the
 *       settler's debit.</li>
 *   <li>All other event types are acknowledged and skipped.</li>
 * </ul>
 */
@Component
public class LedgerProjectionWorker {

    private static final Logger log = LoggerFactory.getLogger(LedgerProjectionWorker.class);

    private final EventRepository        eventRepository;
    private final DraftRepository        draftRepository;
    private final GroupBalanceRepository groupBalanceRepository;

    public LedgerProjectionWorker(EventRepository eventRepository,
                                  DraftRepository draftRepository,
                                  GroupBalanceRepository groupBalanceRepository) {
        this.eventRepository        = eventRepository;
        this.draftRepository        = draftRepository;
        this.groupBalanceRepository = groupBalanceRepository;
    }

    // ─── Incremental projection update ──────────────────────────────────────

    /**
     * Processes all unprocessed events for {@code groupId} since the last
     * known projection version.
     *
     * <p>Called after any write operation or triggered on startup to rebuild
     * stale projections.
     *
     * @param groupId target group
     */
    @Transactional
    public void project(String groupId) {
        long fromVersion = groupBalanceRepository
                .findMaxLastEventVersionByGroupId(groupId)
                .map(v -> v + 1)
                .orElse(1L);

        List<EventEntity> newEvents = eventRepository
                .findByGroupIdAndVersionGreaterThanEqualOrderByVersionAsc(groupId, fromVersion);

        if (newEvents.isEmpty()) {
            log.debug("[LedgerProjection] No new events for groupId={} since version={}", groupId, fromVersion);
            return;
        }

        log.info("[LedgerProjection] Processing {} new events for groupId={}", newEvents.size(), groupId);

        for (EventEntity event : newEvents) {
            try {
                handleEvent(event);
            } catch (Exception e) {
                log.error("[LedgerProjection] Failed to process event id={} type={}: {}",
                        event.getId(), event.getEventType(), e.getMessage(), e);
                // Skip bad events rather than stalling the projection
            }
        }
    }

    // ─── Event handlers ──────────────────────────────────────────────────────

    private void handleEvent(EventEntity event) {
        switch (event.getEventType()) {
            case "DraftApproved"      -> applyDraftApproved(event);
            case "SettlementRecorded" -> applySettlementRecorded(event);
            default -> log.debug("[LedgerProjection] Skipping event type={} id={}", event.getEventType(), event.getId());
        }
    }

    private void applyDraftApproved(EventEntity event) {
        // Fetch the approved draft
        String draftId = extractField(event.getPayload(), "draftId");
        if (draftId == null) return;

        Optional<DraftEntity> draftOpt = draftRepository.findById(draftId);
        if (draftOpt.isEmpty()) {
            log.warn("[LedgerProjection] DraftApproved event references unknown draft id={}", draftId);
            return;
        }

        DraftEntity draft = draftOpt.get();
        BigDecimal total = BigDecimal.valueOf(draft.getAmountMinor(), 2);

        // Participants (non-payer members sharing the expense)
        String[] participants = draft.getParticipants() != null && !draft.getParticipants().isBlank()
                ? draft.getParticipants().split(",")
                : new String[0];

        int memberCount = participants.length + 1; // +1 for payer
        BigDecimal share = memberCount > 0
                ? total.divide(BigDecimal.valueOf(memberCount), 2, java.math.RoundingMode.HALF_UP)
                : total;

        String groupId = draft.getGroupId();
        String currency = draft.getCurrency();

        // Credit payer (they paid the full amount)
        GroupBalanceEntity payerBalance = getOrCreate(groupId, draft.getPayerIdentifier(),
                draft.getPayerIdentifier(), currency);
        payerBalance.recordPayment(total);
        payerBalance.recordOwed(share);     // payer also owes their own share
        payerBalance.setLastEventVersion(event.getVersion());
        groupBalanceRepository.save(payerBalance);

        // Debit each non-payer participant
        for (String participant : participants) {
            String p = participant.trim();
            if (p.isBlank()) continue;
            GroupBalanceEntity participantBalance = getOrCreate(groupId, p, p, currency);
            participantBalance.recordOwed(share);
            participantBalance.setLastEventVersion(event.getVersion());
            groupBalanceRepository.save(participantBalance);
        }

        log.info("[LedgerProjection] DraftApproved applied — draftId={} total={} members={}",
                draftId, total, memberCount);
    }

    private void applySettlementRecorded(EventEntity event) {
        String fromMember = extractField(event.getPayload(), "from");
        String toMember   = extractField(event.getPayload(), "to");
        String amountStr  = extractField(event.getPayload(), "amount");
        String groupId    = event.getGroupId();

        if (fromMember == null || toMember == null || amountStr == null) return;

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
        } catch (NumberFormatException e) {
            log.warn("[LedgerProjection] Invalid amount in SettlementRecorded: {}", amountStr);
            return;
        }

        // "from" pays "to": from's owed decreases, to's credit decreases
        Optional<GroupBalanceEntity> fromOpt = groupBalanceRepository.findByGroupIdAndMemberId(groupId, fromMember);
        Optional<GroupBalanceEntity> toOpt   = groupBalanceRepository.findByGroupIdAndMemberId(groupId, toMember);

        fromOpt.ifPresent(b -> {
            b.recordPayment(amount);
            b.setLastEventVersion(event.getVersion());
            groupBalanceRepository.save(b);
        });

        toOpt.ifPresent(b -> {
            b.recordOwed(amount);
            b.setLastEventVersion(event.getVersion());
            groupBalanceRepository.save(b);
        });

        log.info("[LedgerProjection] Settlement applied — from={} to={} amount={} groupId={}",
                fromMember, toMember, amount, groupId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private GroupBalanceEntity getOrCreate(String groupId, String memberId,
                                           String memberName, String currency) {
        return groupBalanceRepository
                .findByGroupIdAndMemberId(groupId, memberId)
                .orElseGet(() -> new GroupBalanceEntity(groupId, memberId, memberName, currency));
    }

    /**
     * Naive JSON field extractor — avoids pulling in a full ObjectMapper here.
     * Looks for {@code "key":"value"} or {@code "key":value} patterns.
     */
    private String extractField(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        if (start >= json.length()) return null;
        char first = json.charAt(start);
        if (first == '"') {
            int end = json.indexOf('"', start + 1);
            return end > start ? json.substring(start + 1, end) : null;
        } else {
            int end = json.indexOf(',', start);
            if (end < 0) end = json.indexOf('}', start);
            return end > start ? json.substring(start, end).trim() : null;
        }
    }
}
