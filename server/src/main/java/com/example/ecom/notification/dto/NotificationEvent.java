package com.example.ecom.notification.dto;

import com.example.ecom.common.enums.NotificationType;

import java.util.UUID;

public record NotificationEvent(
        String recipientType,
        UUID recipientId,
        NotificationType type,
        String message
) {
}
