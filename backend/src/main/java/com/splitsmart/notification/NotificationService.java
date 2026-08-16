package com.splitsmart.notification;

import org.springframework.stereotype.Service;

/**
 * Notification & WebSocket Module Boundary
 * Responsible for push notifications (FCM), WebSockets/SSE draft review updates, and participant alerts.
 */
@Service
public class NotificationService {
    public String getModuleInfo() {
        return "Notification & WebSocket Module Initialized";
    }
}
