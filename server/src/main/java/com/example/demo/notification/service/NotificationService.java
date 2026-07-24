package com.example.demo.notification.service;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.enums.NotificationType;
import com.example.demo.common.enums.Permission;
import com.example.demo.notification.dto.ClientConnection;
import com.example.demo.notification.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    private final Map<Long, ClientConnection> connections = new ConcurrentHashMap<>();

    public SseEmitter subscribe(CustomUserDetails userDetails) {
        Long userId = userDetails.getId();

        Set<Permission> permissions = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(Permission::fromValue)
                .collect(Collectors.toUnmodifiableSet());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> removeConnection(userId));
        emitter.onTimeout(() -> removeConnection(userId));
        emitter.onError(ex -> removeConnection(userId));

        ClientConnection previous = connections.put(
                userId,
                new ClientConnection(emitter, permissions)
        );

        if (previous != null) {
            previous.emitter().complete();
        }

        sendEvent(userId, "init", new NotificationResponse(NotificationType.SUCCESS, "Connected"));

        log.info("User {} subscribed with permissions {}", userId, permissions);

        return emitter;
    }

    public void sendToUser(Long userId, NotificationResponse notificationResponse) {
        ClientConnection connection = connections.get(userId);

        if (connection != null) {
            sendEvent(userId, "notification", notificationResponse);
        }
    }

    public void sendToAdmins(NotificationResponse notificationResponse) {
        connections.forEach((userId, connection) -> {
            if (connection.permissions().contains(Permission.ADMIN_ACCESS)
                    || connection.permissions().contains(Permission.SUPER_ADMIN_ACCESS)) {

                sendEvent(userId, "notification", notificationResponse);
            }
        });
    }

    @Scheduled(fixedRate = 30000)
    public void heartbeat() {
        connections.forEach((userId, connection) ->
                sendEvent(userId, "heartbeat", new NotificationResponse(NotificationType.SUCCESS, "")));
    }

    private void sendEvent(Long userId, String eventName, NotificationResponse notificationResponse) {
        ClientConnection connection = connections.get(userId);

        if (connection == null) {
            return;
        }

        try {
            connection.emitter().send(
                    SseEmitter.event()
                            .name(eventName)
                            .data(notificationResponse)
            );
        } catch (Exception ex) {
            log.info("Removing broken SSE connection for user {}", userId, ex);
            removeConnection(userId);
        }
    }

    private void removeConnection(Long userId) {
        ClientConnection connection = connections.remove(userId);

        if (connection != null) {
            connection.emitter().complete();
        }
    }
}