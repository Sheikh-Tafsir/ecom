package com.example.ecom.notification.dto;

import com.example.ecom.common.enums.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        NotificationType type,
        String message,
        Instant createdAt
) {
    public NotificationResponse(NotificationType type, String message) {
        this(type, message, Instant.now());
    }
}
