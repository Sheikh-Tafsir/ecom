package com.example.ecom.common.service.notification;

import com.example.ecom.common.event.EventConstants;
import com.example.ecom.common.event.RedisEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static com.example.ecom.common.event.EventConstants.NOTIFICATION_CHANNEL;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisNotificationPublisherTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RedisEventPublisher redisEventPublisher;

    @Test
    @DisplayName("publish with NOTIFICATION event type should send to NOTIFICATION_CHANNEL")
    void publish_notificationEvent_sendsToNotificationChannel() throws Exception {
        String payload = "{\"recipientId\":\"123\",\"message\":\"Hello\"}";
        when(objectMapper.writeValueAsString("test-payload")).thenReturn(payload);

        redisEventPublisher.publish(EventConstants.SOURCE_NOTIFICATION, EventConstants.NOTIFICATION, "test-payload");

        verify(stringRedisTemplate).convertAndSend(eq(NOTIFICATION_CHANNEL), eq(payload));
    }

    @Test
    @DisplayName("publish with domain event type should send to prefixed channel")
    void publish_domainEvent_sendsToPrefixedChannel() throws Exception {
        String payload = "{\"orderId\":\"abc\"}";
        when(objectMapper.writeValueAsString("test-payload")).thenReturn(payload);

        redisEventPublisher.publish(EventConstants.SOURCE_ORDER, EventConstants.ORDER_STOCK_RELEASE, "test-payload");

        verify(stringRedisTemplate).convertAndSend(eq("ecom:events:" + EventConstants.ORDER_STOCK_RELEASE), eq(payload));
    }
}
