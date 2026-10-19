package com.example.ecom.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import static com.example.ecom.notification.service.NotificationService.NOTIFICATION_CHANNEL;

@Component
@ConditionalOnBean(StringRedisTemplate.class)
@RequiredArgsConstructor
public class RedisNotificationPublisher implements NotificationPublisher{

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void publish(String event) {
        stringRedisTemplate.convertAndSend(NOTIFICATION_CHANNEL, event);
    }
}
