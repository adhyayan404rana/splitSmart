package com.splitsmart.notification;

/**
 * Event-driven alert dispatch interface for SplitSmart notifications.
 *
 * <p>Implementations of this interface deliver real-time alerts to group
 * members whenever a ledger event occurs (draft created, approved, disputed,
 * settled, etc.).
 *
 * <p>The primary implementation is {@link SseNotificationService}, which
 * pushes events over Server-Sent Events (SSE) to connected browser clients.
 * Additional implementations (WebSocket, Firebase Cloud Messaging, email)
 * can be wired in without changing the call sites in the service layer.
 *
 * <h3>Notification event types</h3>
 * <ul>
 *   <li>{@code DRAFT_CREATED}   — a new expense draft needs approval</li>
 *   <li>{@code DRAFT_APPROVED}  — a draft received a new approval</li>
 *   <li>{@code DRAFT_FINALIZED} — quorum reached; draft is approved</li>
 *   <li>{@code DRAFT_DISPUTED}  — a member raised a dispute</li>
 *   <li>{@code DRAFT_MODIFIED}  — a draft was edited and reset</li>
 *   <li>{@code SETTLEMENT_DONE} — a debt payment was recorded</li>
 *   <li>{@code HEARTBEAT}       — keep-alive ping for SSE connections</li>
 * </ul>
 */
public interface NotificationService {

    /**
     * Pushes an event to all currently connected members of {@code groupId}.
     *
     * @param groupId   the target group
     * @param eventType one of the type constants defined above
     * @param payload   arbitrary JSON-serializable object attached to the event
     */
    void notifyGroup(String groupId, String eventType, Object payload);

    /**
     * Pushes an event to a single specific user by their user ID.
     *
     * @param userId    target user
     * @param eventType notification type
     * @param payload   arbitrary JSON-serializable object
     */
    void notifyUser(String userId, String eventType, Object payload);

    /**
     * Sends a heartbeat to all active connections to prevent proxy timeouts.
     * Typically called by a {@code @Scheduled} task every 20–30 seconds.
     */
    void sendHeartbeat();

    // ─── Event type constants ─────────────────────────────────────────────────

    String DRAFT_CREATED   = "DRAFT_CREATED";
    String DRAFT_APPROVED  = "DRAFT_APPROVED";
    String DRAFT_FINALIZED = "DRAFT_FINALIZED";
    String DRAFT_DISPUTED  = "DRAFT_DISPUTED";
    String DRAFT_MODIFIED  = "DRAFT_MODIFIED";
    String SETTLEMENT_DONE = "SETTLEMENT_DONE";
    String HEARTBEAT       = "HEARTBEAT";
}
