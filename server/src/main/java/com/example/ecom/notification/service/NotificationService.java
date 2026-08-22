package com.example.ecom.notification.service;

import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.enums.NotificationType;
import com.example.ecom.common.enums.Permission;
import com.example.ecom.notification.dto.ClientConnection;
import com.example.ecom.notification.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationService {

    private static final long SSE_TIMEOUT = Duration.ofMinutes(30).toMillis();

    private final Map<Long, ClientConnection> connections = new ConcurrentHashMap<>();

    public SseEmitter subscribe(CustomUserDetails userDetails) {
        Long userId = userDetails.getId();

        Set<Permission> permissions = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(Permission::fromValue)
                .collect(Collectors.toUnmodifiableSet());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> removeConnection(userId, emitter));
        emitter.onTimeout(() -> removeConnection(userId, emitter));
        emitter.onError(ex -> removeConnection(userId, emitter));

        ClientConnection previous = connections.put(
                userId,
                new ClientConnection(emitter, permissions)
        );

        if (previous != null) {
            try {
                previous.emitter().complete();
            } catch (Exception ignored) {}
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

        synchronized (connection.emitter()) {
            try {
                connection.emitter().send(
                        SseEmitter.event()
                                .name(eventName)
                                .data(notificationResponse)
                );
            } catch (Exception ex) {
                log.info("Removing broken SSE connection for user {}", userId, ex);
                removeConnection(userId, connection.emitter());
            }
        }
    }

    private void removeConnection(Long userId, SseEmitter emitter) {
        connections.computeIfPresent(userId, (key, current) -> {
            if (current.emitter() == emitter) {
                try {
                    emitter.complete();
                } catch (Exception ignored) {}
                return null;
            }
            return current;
        });
    }
}