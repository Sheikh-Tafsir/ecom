package com.example.ecom.notification.service;

import com.example.ecom.common.enums.NotificationType;
import com.example.ecom.common.enums.Permission;
import com.example.ecom.notification.dto.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.example.ecom.common.utils.Utils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Service
public class NotificationService {

    public static final String NOTIFICATION_CHANNEL = "ecom:notifications";

    private final ObjectMapper objectMapper;
    private final NotificationPublisher notificationPublisher;

    public NotificationService(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    @Autowired
    public NotificationService(ObjectMapper objectMapper, @Nullable NotificationPublisher notificationPublisher) {
        this.objectMapper = objectMapper;
        this.notificationPublisher = notificationPublisher;
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
        if (notificationPublisher == null) {
            log.info("NotificationPublisher is null (Redis not enabled). Skipping notification: {}", event);
            return;
        }

        try {
            notificationPublisher.publish(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.warn("Failed to publish notification: {}", e.getMessage(), e);
        }
    }
}