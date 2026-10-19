package com.example.ecom.notification.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static com.example.ecom.notification.service.NotificationService.NOTIFICATION_CHANNEL;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisNotificationPublisherTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private RedisNotificationPublisher redisNotificationPublisher;

    @Test
    @DisplayName("publish should send event string to NOTIFICATION_CHANNEL")
    void publish_sendsToRedisChannel() {
        String payload = "{\"recipientId\":\"123\",\"message\":\"Hello\"}";

        redisNotificationPublisher.publish(payload);

        verify(stringRedisTemplate).convertAndSend(NOTIFICATION_CHANNEL, payload);
    }
}
