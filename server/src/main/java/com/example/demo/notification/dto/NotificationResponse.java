package com.example.demo.notification.dto;

import com.example.demo.common.enums.NotificationType;

public record NotificationResponse(
        NotificationType type,
        String message
) {
}
