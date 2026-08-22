package com.example.ecom.notification.dto;

import com.example.ecom.common.enums.NotificationType;

public record NotificationResponse(
        NotificationType type,
        String message
) {
}
