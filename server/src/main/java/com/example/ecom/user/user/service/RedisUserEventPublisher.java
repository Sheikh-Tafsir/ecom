package com.example.ecom.user.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes user-change events to Redis pub/sub so that downstream services
 * (e.g. chat server) can invalidate their own caches.
 *
 * <p>Only publishes the userId — the subscriber decides what cache keys to evict.
 * This keeps the main server decoupled from the chat server's internal key layout.</p>
 */
@Component
@ConditionalOnBean(StringRedisTemplate.class)
@RequiredArgsConstructor
public class RedisUserEventPublisher implements UserEventPublisher {

    public static final String USER_UPDATES_CHANNEL = "ecom:user-updates";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void publishUserChanged(UUID userId) {
        stringRedisTemplate.convertAndSend(USER_UPDATES_CHANNEL, userId.toString());
    }
}
