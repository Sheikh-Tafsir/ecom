package com.example.ecom.notification.service;

import com.example.ecom.notification.dto.NotificationEvent;
import com.example.ecom.notification.dto.NotificationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    public static final String NOTIFICATION_CHANNEL = "ecom:notifications";

    private final ObjectMapper objectMapper;
    private StringRedisTemplate stringRedisTemplate;

    public NotificationService(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    @Autowired
    public NotificationService(ObjectMapper objectMapper, @Autowired(required = false) StringRedisTemplate stringRedisTemplate) {
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void sendToUser(UUID userId, NotificationResponse notificationResponse) {
        if (userId == null || notificationResponse == null) {
            return;
        }

        NotificationEvent event = new NotificationEvent(
                "USER",
                userId,
                notificationResponse.type(),
                notificationResponse.message(),
                notificationResponse.createdAt() != null ? notificationResponse.createdAt() : Instant.now()
        );

        publishToRedis(event);
    }

    public void sendToAdmins(NotificationResponse notificationResponse) {
        if (notificationResponse == null) {
            return;
        }

        NotificationEvent event = new NotificationEvent(
                "ADMIN",
                null,
                notificationResponse.type(),
                notificationResponse.message(),
                notificationResponse.createdAt() != null ? notificationResponse.createdAt() : Instant.now()
        );

        publishToRedis(event);
    }

    private void publishToRedis(NotificationEvent event) {
        if (stringRedisTemplate == null) {
            log.warn("StringRedisTemplate is null. Unable to publish notification to Redis channel: {}", NOTIFICATION_CHANNEL);
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(NOTIFICATION_CHANNEL, payload);
        } catch (Exception e) {
            log.warn("Failed to publish notification to Redis channel {}: {}", NOTIFICATION_CHANNEL, e.getMessage(), e);
        }
    }
}