package com.example.ecom.user.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static com.example.ecom.user.user.service.RedisUserEventPublisher.USER_UPDATES_CHANNEL;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisUserEventPublisherTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private RedisUserEventPublisher redisUserEventPublisher;

    @Test
    @DisplayName("publishUserChanged should send userId to USER_UPDATES_CHANNEL")
    void publishUserChanged_sendsToRedisChannel() {
        UUID userId = UUID.randomUUID();

        redisUserEventPublisher.publishUserChanged(userId);

        verify(stringRedisTemplate).convertAndSend(USER_UPDATES_CHANNEL, userId.toString());
    }
}
