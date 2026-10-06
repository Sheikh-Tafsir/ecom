package com.example.ecom.notification.dto;

import com.example.ecom.common.enums.NotificationType;

public record NotificationEvent(
        String recipientType,
        Long recipientId,
        NotificationType type,
        String message
) {
}
