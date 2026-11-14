package com.example.ecom.common.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResilientCacheManagerTest {

    @Mock
    private CacheManager delegate;

    @Mock
    private Cache redisCache;

    private ResilientCacheManager manager;

    @BeforeEach
    void setUp() {
        Map<String, Duration> ttlMap = Map.of(
                "products", Duration.ofMinutes(15),
                "idempotency", Duration.ofMinutes(60)
        );
        manager = new ResilientCacheManager(
                delegate, ttlMap, Duration.ofMinutes(5), Set.of("idempotency"));
    }

    @Test
    void getCache_forNormalCache_returnsResilientCache() {
        when(delegate.getCache("products")).thenReturn(redisCache);
        when(redisCache.getName()).thenReturn("products");

        Cache cache = manager.getCache("products");

        assertNotNull(cache);
        assertInstanceOf(ResilientCache.class, cache);
        assertEquals("products", cache.getName());
    }

    @Test
    void getCache_forFailClosedCache_returnsUnwrappedRedisCache() {
        when(delegate.getCache("idempotency")).thenReturn(redisCache);

        Cache cache = manager.getCache("idempotency");

        assertSame(redisCache, cache);
    }

    @Test
    void getCache_returnsSameInstanceOnRepeatedCalls() {
        when(delegate.getCache("products")).thenReturn(redisCache);

        Cache first = manager.getCache("products");
        Cache second = manager.getCache("products");

        assertSame(first, second);
    }

    @Test
    void getCache_whenDelegateReturnsNull_returnsNull() {
        when(delegate.getCache("unknown")).thenReturn(null);

        Cache cache = manager.getCache("unknown");

        assertNull(cache);
    }

    @Test
    void getCacheNames_delegatesToUnderlying() {
        when(delegate.getCacheNames()).thenReturn(List.of("products", "idempotency"));

        assertEquals(List.of("products", "idempotency"), List.copyOf(manager.getCacheNames()));
    }
}
