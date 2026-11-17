package com.example.ecom.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import static com.example.ecom.common.event.EventConstants.*;

/**
 * Redis pub/sub implementation of {@link EventPublisher}.
 * <p>
 * Used for real-time notifications to the chat-server (Node.js) via Redis pub/sub.
 * Also serves as a fallback for domain events when RabbitMQ is not available.
 * <p>
 * Unlike {@link RMqEventPublisher}, this does NOT wait for transaction commit
 * and does NOT guarantee durable delivery.
 */
@Slf4j
@Component
@ConditionalOnBean(StringRedisTemplate.class)
@RequiredArgsConstructor
public class RedisEventPublisher implements EventPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public <T> void publish(String source, String eventType, T payload) {
        String channel = resolveChannel(eventType);

        try {
            String json = objectMapper.writeValueAsString(payload);
            stringRedisTemplate.convertAndSend(channel, json);
            log.info("Published event [{}] to Redis channel {}", eventType, channel);
        } catch (Exception e) {
            log.error("Failed to publish event [{}] to Redis: {}",
                    eventType, e.getMessage(), e);
        }
    }

    /**
     * Notification events go to the well-known channel the chat-server subscribes to.
     * All other events use a channel derived from the event type.
     */
    private String resolveChannel(String eventType) {
        if (NOTIFICATION.equals(eventType)) {
            return NOTIFICATION_CHANNEL;
        }
        return REDIS_EVENT_CHANNEL_PREFIX + eventType;
    }
}
