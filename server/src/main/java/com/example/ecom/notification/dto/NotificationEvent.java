package com.example.ecom.notification.dto;

import com.example.ecom.common.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationEvent(
        String recipientType,
        UUID recipientId,
        NotificationType type,
        String message,
        Instant createdAt
) {
    public NotificationEvent(String recipientType, UUID recipientId, NotificationType type, String message) {
        this(recipientType, recipientId, type, message, Instant.now());
    }
}
