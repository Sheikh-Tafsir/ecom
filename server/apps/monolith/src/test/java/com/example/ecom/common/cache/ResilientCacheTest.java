package com.example.ecom.common.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResilientCacheTest {

    @Mock
    private Cache redisCache;

    @Mock
    private Cache localCache;

    private ResilientCache resilientCache;

    @BeforeEach
    void setUp() {
        lenient().when(redisCache.getName()).thenReturn("testCache");
        resilientCache = new ResilientCache(redisCache, localCache);
    }

    // ── GET (ValueWrapper) ──────────────────────────────────────

    @Test
    void get_whenRedisReturnsValue_returnsItAndSyncsToL1() {
        Cache.ValueWrapper wrapper = mock(Cache.ValueWrapper.class);
        when(wrapper.get()).thenReturn("redis-value");
        when(redisCache.get("key1")).thenReturn(wrapper);

        Cache.ValueWrapper result = resilientCache.get("key1");

        assertNotNull(result);
        assertEquals("redis-value", result.get());
        verify(localCache).put("key1", "redis-value");
    }

    @Test
    void get_whenRedisReturnsNull_fallsBackToL1() {
        when(redisCache.get("key1")).thenReturn(null);

        Cache.ValueWrapper localWrapper = mock(Cache.ValueWrapper.class);
        when(localCache.get("key1")).thenReturn(localWrapper);

        Cache.ValueWrapper result = resilientCache.get("key1");

        assertSame(localWrapper, result);
    }

    @Test
    void get_whenRedisThrows_fallsBackToL1() {
        when(redisCache.get("key1")).thenThrow(new RuntimeException("Connection refused"));

        Cache.ValueWrapper localWrapper = mock(Cache.ValueWrapper.class);
        when(localCache.get("key1")).thenReturn(localWrapper);

        Cache.ValueWrapper result = resilientCache.get("key1");

        assertSame(localWrapper, result);
    }

    // ── GET (typed) ─────────────────────────────────────────────

    @Test
    void getTyped_whenRedisReturnsValue_returnsItAndSyncsToL1() {
        when(redisCache.get("key1", String.class)).thenReturn("redis-typed");

        String result = resilientCache.get("key1", String.class);

        assertEquals("redis-typed", result);
        verify(localCache).put("key1", "redis-typed");
    }

    @Test
    void getTyped_whenRedisReturnsNull_fallsBackToL1() {
        when(redisCache.get("key1", String.class)).thenReturn(null);
        when(localCache.get("key1", String.class)).thenReturn("local-typed");

        String result = resilientCache.get("key1", String.class);

        assertEquals("local-typed", result);
    }

    @Test
    void getTyped_whenRedisThrows_fallsBackToL1() {
        when(redisCache.get("key1", String.class)).thenThrow(new RuntimeException("Timeout"));
        when(localCache.get("key1", String.class)).thenReturn("local-typed");

        String result = resilientCache.get("key1", String.class);

        assertEquals("local-typed", result);
    }

    // ── GET (Callable) ──────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getCallable_whenRedisReturnsValue_returnsItAndSyncsToL1() {
        Callable<String> loader = mock(Callable.class);
        when(redisCache.get(eq("key1"), eq(loader))).thenReturn("loaded");

        String result = resilientCache.get("key1", loader);

        assertEquals("loaded", result);
        verify(localCache).put("key1", "loaded");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getCallable_whenRedisThrows_fallsBackToL1WithLoader() {
        Callable<String> loader = mock(Callable.class);
        when(redisCache.get(eq("key1"), eq(loader))).thenThrow(new RuntimeException("Redis down"));
        when(localCache.get(eq("key1"), eq(loader))).thenReturn("local-loaded");

        String result = resilientCache.get("key1", loader);

        assertEquals("local-loaded", result);
    }

    // ── PUT ─────────────────────────────────────────────────────

    @Test
    void put_whenRedisHealthy_writesToBoth() {
        resilientCache.put("key1", "value1");

        verify(redisCache).put("key1", "value1");
        verify(localCache).put("key1", "value1");
    }

    @Test
    void put_whenRedisThrows_stillWritesToL1() {
        doThrow(new RuntimeException("Connection lost")).when(redisCache).put(any(), any());

        resilientCache.put("key1", "value1");

        verify(localCache).put("key1", "value1");
    }

    // ── PUT IF ABSENT ───────────────────────────────────────────

    @Test
    void putIfAbsent_whenKeyExists_syncsExistingToL1() {
        Cache.ValueWrapper existing = mock(Cache.ValueWrapper.class);
        when(existing.get()).thenReturn("existing-val");
        when(redisCache.putIfAbsent("key1", "new-val")).thenReturn(existing);

        Cache.ValueWrapper result = resilientCache.putIfAbsent("key1", "new-val");

        assertNotNull(result);
        verify(localCache).put("key1", "existing-val");
    }

    @Test
    void putIfAbsent_whenKeyAbsent_syncsNewValueToL1() {
        when(redisCache.putIfAbsent("key1", "new-val")).thenReturn(null);

        Cache.ValueWrapper result = resilientCache.putIfAbsent("key1", "new-val");

        assertNull(result);
        verify(localCache).put("key1", "new-val");
    }

    @Test
    void putIfAbsent_whenRedisThrows_fallsBackToL1() {
        when(redisCache.putIfAbsent("key1", "new-val")).thenThrow(new RuntimeException("Timeout"));
        when(localCache.putIfAbsent("key1", "new-val")).thenReturn(null);

        Cache.ValueWrapper result = resilientCache.putIfAbsent("key1", "new-val");

        assertNull(result);
        verify(localCache).putIfAbsent("key1", "new-val");
    }

    // ── EVICT ───────────────────────────────────────────────────

    @Test
    void evict_evictsFromBoth() {
        resilientCache.evict("key1");

        verify(redisCache).evict("key1");
        verify(localCache).evict("key1");
    }

    @Test
    void evict_whenRedisThrows_stillEvictsFromL1() {
        doThrow(new RuntimeException("Redis down")).when(redisCache).evict(any());

        resilientCache.evict("key1");

        verify(localCache).evict("key1");
    }

    // ── CLEAR ───────────────────────────────────────────────────

    @Test
    void clear_clearsBoth() {
        resilientCache.clear();

        verify(redisCache).clear();
        verify(localCache).clear();
    }

    @Test
    void clear_whenRedisThrows_stillClearsL1() {
        doThrow(new RuntimeException("Redis down")).when(redisCache).clear();

        resilientCache.clear();

        verify(localCache).clear();
    }

    // ── METADATA ────────────────────────────────────────────────

    @Test
    void getName_delegatesToRedis() {
        assertEquals("testCache", resilientCache.getName());
    }

    @Test
    void getNativeCache_delegatesToRedis() {
        Object nativeCache = new Object();
        when(redisCache.getNativeCache()).thenReturn(nativeCache);

        assertSame(nativeCache, resilientCache.getNativeCache());
    }

    // ── EVICT IF PRESENT ────────────────────────────────────────

    @Test
    void evictIfPresent_evictsFromBoth() {
        when(redisCache.evictIfPresent("key1")).thenReturn(true);

        boolean result = resilientCache.evictIfPresent("key1");

        assertTrue(result);
        verify(localCache).evictIfPresent("key1");
    }

    @Test
    void evictIfPresent_whenRedisThrows_stillEvictsFromL1() {
        when(redisCache.evictIfPresent("key1")).thenThrow(new RuntimeException("Redis down"));

        boolean result = resilientCache.evictIfPresent("key1");

        assertFalse(result);
        verify(localCache).evictIfPresent("key1");
    }

    // ── INVALIDATE ──────────────────────────────────────────────

    @Test
    void invalidate_invalidatesBoth() {
        when(redisCache.invalidate()).thenReturn(true);

        boolean result = resilientCache.invalidate();

        assertTrue(result);
        verify(localCache).invalidate();
    }

    @Test
    void invalidate_whenRedisThrows_stillInvalidatesL1() {
        when(redisCache.invalidate()).thenThrow(new RuntimeException("Redis down"));

        boolean result = resilientCache.invalidate();

        assertFalse(result);
        verify(localCache).invalidate();
    }
}
