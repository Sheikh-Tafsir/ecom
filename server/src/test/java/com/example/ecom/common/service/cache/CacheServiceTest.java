package com.example.ecom.common.service.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    private InMemoryCacheService inMemoryService;

    @Mock
    private RedisCacheService mockRedisService;

    @BeforeEach
    void setUp() {
        inMemoryService = new InMemoryCacheService();
    }

    @Test
    void inMemoryCache_putAndGet_returnsValue() {
        inMemoryService.putToken("key1", "token123", Duration.ofMinutes(10));
        assertEquals("token123", inMemoryService.getToken("key1"));
    }

    @Test
    void inMemoryCache_whenExpired_returnsNull() throws InterruptedException {
        // Expire immediately with 1 millisecond
        inMemoryService.putToken("key2", "token-expired", Duration.ofMillis(1));
        Thread.sleep(10);
        assertNull(inMemoryService.getToken("key2"));
    }

    @Test
    void inMemoryCache_whenKeyNotFound_returnsNull() {
        assertNull(inMemoryService.getToken("non-existent"));
        assertNull(inMemoryService.getToken(null));
    }

    @Test
    void resilientCache_whenRedisHealthy_returnsFromRedis() {
        when(mockRedisService.getToken("test:key")).thenReturn("redis-token");

        ResilientCacheService resilientService = new ResilientCacheService(mockRedisService, inMemoryService, "redis");
        String result = resilientService.getToken("test:key");

        assertEquals("redis-token", result);
        verify(mockRedisService).getToken("test:key");
    }

    @Test
    void resilientCache_whenRedisFails_fallsBackToInMemory() {
        when(mockRedisService.getToken("test:key")).thenThrow(new RuntimeException("Redis connection refused"));
        inMemoryService.putToken("test:key", "fallback-token", Duration.ofMinutes(10));

        ResilientCacheService resilientService = new ResilientCacheService(mockRedisService, inMemoryService, "redis");
        String result = resilientService.getToken("test:key");

        assertEquals("fallback-token", result);
    }

    @Test
    void resilientCache_whenStrategyNone_doesNotCallRedis() {
        inMemoryService.putToken("test:key", "local-token", Duration.ofMinutes(10));

        ResilientCacheService resilientService = new ResilientCacheService(mockRedisService, inMemoryService, "none");
        String result = resilientService.getToken("test:key");

        assertEquals("local-token", result);
        verifyNoInteractions(mockRedisService);

        resilientService.putToken("test:key2", "local-token2", Duration.ofMinutes(10));
        verifyNoInteractions(mockRedisService);
        assertEquals("local-token2", inMemoryService.getToken("test:key2"));
    }

    @Test
    void resilientCache_putToken_storesInBothForImmediateResilience() {
        ResilientCacheService resilientService = new ResilientCacheService(mockRedisService, inMemoryService, "redis");
        resilientService.putToken("key", "val", Duration.ofMinutes(5));

        verify(mockRedisService).putToken(eq("key"), eq("val"), eq(Duration.ofMinutes(5)));
        assertEquals("val", inMemoryService.getToken("key"));
    }
}
