package com.example.ecom.common.service.notification;

import com.example.ecom.common.enums.NotificationType;
import com.example.ecom.common.enums.Permission;
import com.example.ecom.common.dto.NotificationEvent;
import com.example.ecom.common.event.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.example.ecom.common.event.EventConstants.NOTIFICATION;
import static com.example.ecom.common.event.EventConstants.SOURCE_NOTIFICATION;
import static com.example.ecom.common.utils.Utils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Service
public class NotificationService {

    private final EventPublisher eventPublisher;

    public NotificationService() {
        this(null);
    }

    @Autowired
    public NotificationService(@Nullable @Qualifier("redisEventPublisher") EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void sendToUser(UUID userId, String message, NotificationType notificationType) {
        if (userId == null || !hasText(message) || notificationType == null) {
            return;
        }

        NotificationEvent event = new NotificationEvent(
                userId,
                null,
                notificationType,
                message
        );

        publish(event);
    }

    public void sendToPermissions(List<Permission> permissions, String message, NotificationType notificationType) {
        if (isEmpty(permissions) || !hasText(message) || notificationType == null) {
            return;
        }

        NotificationEvent event = new NotificationEvent(
                null,
                permissions,
                notificationType,
                message
        );

        publish(event);
    }

    public void sendToAdmins(String message, NotificationType notificationType) {
        sendToPermissions(List.of(Permission.SUPER_ADMIN_ACCESS, Permission.ADMIN_ACCESS), message, notificationType);
    }

    private void publish(NotificationEvent event) {
        if (eventPublisher == null) {
            log.info("EventPublisher is null. Skipping notification: {}", event);
            return;
        }

        try {
            eventPublisher.publish(SOURCE_NOTIFICATION, NOTIFICATION, event);
        } catch (Exception e) {
            log.warn("Failed to publish notification: {}", e.getMessage(), e);
        }
    }
}
