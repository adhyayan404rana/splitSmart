package com.splitsmart.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * SSE-based implementation of {@link NotificationService}.
 *
 * <p>Maintains a registry of per-user {@link SseEmitter} instances. When
 * an event is published, it is serialized to JSON and sent to all emitters
 * belonging to the target group or user.
 *
 * <h3>Connection lifecycle</h3>
 * <ol>
 *   <li>Client calls {@code GET /notifications/stream?userId={id}&groupId={id}}.</li>
 *   <li>{@link NotificationController} calls {@link #subscribe} to obtain an emitter.</li>
 *   <li>The emitter is stored in {@code userEmitters} (keyed by userId) and
 *       {@code groupMembers} (keyed by groupId → Set of userIds).</li>
 *   <li>On completion or timeout the emitter is removed from both maps.</li>
 * </ol>
 *
 * <h3>Heartbeat</h3>
 * A scheduled heartbeat fires every 25 seconds to keep proxy connections
 * alive and detect stale emitters early.
 *
 * <h3>Scalability note</h3>
 * This in-process implementation works for a single node. For multi-node
 * deployments, replace the local maps with a Redis Pub/Sub fan-out layer.
 */
@Service
public class SseNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(SseNotificationService.class);

    /** SSE connection timeout: 5 minutes. Clients reconnect automatically. */
    private static final long SSE_TIMEOUT_MS = 5 * 60 * 1_000L;

    /** userId → Set of active SseEmitters (one per browser tab/session). */
    private final Map<String, Set<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    /** groupId → Set of userIds currently connected. */
    private final Map<String, Set<String>> groupMembers = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public SseNotificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ─── Subscribe ────────────────────────────────────────────────────────────

    /**
     * Creates and registers a new SSE emitter for the given user and group.
     *
     * @param userId  the authenticated user
     * @param groupId the group the user is viewing
     * @return a configured {@link SseEmitter} ready to send events
     */
    public SseEmitter subscribe(String userId, String groupId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(emitter);
        groupMembers.computeIfAbsent(groupId, k -> new CopyOnWriteArraySet<>()).add(userId);

        Runnable cleanup = () -> removeEmitter(userId, groupId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> {
            log.debug("[SseNotification] Emitter error for userId={}: {}", userId, e.getMessage());
            cleanup.run();
        });

        // Send immediate connected event
        sendToEmitter(emitter, NotificationService.HEARTBEAT,
                Map.of("status", "connected", "userId", userId, "groupId", groupId));

        log.info("[SseNotification] Subscribed userId={} to groupId={} — active connections: {}",
                userId, groupId, countActive());
        return emitter;
    }

    // ─── NotificationService implementation ──────────────────────────────────

    @Override
    public void notifyGroup(String groupId, String eventType, Object payload) {
        Set<String> members = groupMembers.getOrDefault(groupId, Set.of());
        if (members.isEmpty()) {
            log.debug("[SseNotification] No active connections for groupId={}", groupId);
            return;
        }
        int sent = 0;
        for (String userId : members) {
            sent += notifyUserInternal(userId, eventType, payload);
        }
        log.info("[SseNotification] Group notification sent — groupId={} type={} recipients={}",
                groupId, eventType, sent);
    }

    @Override
    public void notifyUser(String userId, String eventType, Object payload) {
        int sent = notifyUserInternal(userId, eventType, payload);
        log.debug("[SseNotification] User notification sent — userId={} type={} emitters={}", userId, eventType, sent);
    }

    @Override
    @Scheduled(fixedDelay = 25_000)
    public void sendHeartbeat() {
        Map<String, Object> ping = Map.of("ts", Instant.now().toString());
        int total = 0;
        for (Map.Entry<String, Set<SseEmitter>> entry : userEmitters.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                sendToEmitter(emitter, HEARTBEAT, ping);
                total++;
            }
        }
        if (total > 0) log.debug("[SseNotification] Heartbeat sent to {} emitters", total);
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private int notifyUserInternal(String userId, String eventType, Object payload) {
        Set<SseEmitter> emitters = userEmitters.getOrDefault(userId, Set.of());
        int count = 0;
        for (SseEmitter emitter : emitters) {
            sendToEmitter(emitter, eventType, payload);
            count++;
        }
        return count;
    }

    private void sendToEmitter(SseEmitter emitter, String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            emitter.send(SseEmitter.event().name(eventType).data(json));
        } catch (JsonProcessingException e) {
            log.error("[SseNotification] Failed to serialize payload: {}", e.getMessage());
        } catch (IOException e) {
            // Emitter is broken; cleanup will be triggered by onError handler
            log.debug("[SseNotification] Send failed (likely disconnected): {}", e.getMessage());
        }
    }

    private void removeEmitter(String userId, String groupId, SseEmitter emitter) {
        Set<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
                Set<String> members = groupMembers.get(groupId);
                if (members != null) {
                    members.remove(userId);
                    if (members.isEmpty()) groupMembers.remove(groupId);
                }
            }
        }
        log.debug("[SseNotification] Emitter removed — userId={} groupId={} remaining={}",
                userId, groupId, countActive());
    }

    private long countActive() {
        return userEmitters.values().stream().mapToLong(Set::size).sum();
    }
}
