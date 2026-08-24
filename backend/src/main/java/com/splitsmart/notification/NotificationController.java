package com.splitsmart.notification;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller exposing the SSE notification stream endpoint.
 *
 * <p>Clients establish a long-lived HTTP connection to this endpoint and
 * receive server-sent events whenever a ledger mutation occurs in their
 * active group (draft created, approved, disputed, settled, etc.).
 *
 * <h3>Endpoint</h3>
 * <pre>
 *   GET /notifications/stream?userId={userId}&groupId={groupId}
 * </pre>
 *
 * <h3>Response format</h3>
 * Each event is sent in the SSE wire format:
 * <pre>
 *   event: DRAFT_APPROVED
 *   data: {"draftId":"abc","approvalCount":2,"requiredApprovals":3}
 * </pre>
 *
 * <h3>Client reconnection</h3>
 * The browser's native {@code EventSource} API automatically reconnects
 * after the server closes the stream (connection timeout after 5 minutes)
 * using exponential backoff. No additional client-side logic is required.
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final SseNotificationService sseService;

    public NotificationController(SseNotificationService sseService) {
        this.sseService = sseService;
    }

    /**
     * Opens an SSE stream for the given user scoped to a specific group.
     *
     * <p>Produces {@code text/event-stream} — required for {@code EventSource}
     * compatibility. The connection stays open until the server closes it
     * (after the configured timeout) or the client disconnects.
     *
     * @param userId  authenticated user's ID (passed as a query param for demo;
     *                replace with {@code @AuthenticationPrincipal} in production)
     * @param groupId the group whose events the client wants to receive
     * @return a {@link SseEmitter} that Spring MVC holds open
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam String userId,
            @RequestParam String groupId) {
        return sseService.subscribe(userId, groupId);
    }
}
