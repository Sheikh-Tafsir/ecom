package com.example.ecom.notification.dto;

import com.example.ecom.common.enums.NotificationType;
import com.example.ecom.common.enums.Permission;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NotificationEvent(
        UUID recipientId,
        List<Permission> targetPermissions,
        NotificationType type,
        String message,
        Instant createdAt
) {
    public NotificationEvent(UUID recipientId, List<Permission> targetPermissions, NotificationType type, String message) {
        this(recipientId, targetPermissions, type, message, Instant.now());
    }
}
