package com.example.ecom.common.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static com.example.ecom.common.utils.CacheConstants.CACHE_IDEMPOTENCY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        when(cacheManager.getCache(CACHE_IDEMPOTENCY)).thenReturn(cache);
    }

    @Test
    void getCachedResponse_found_returnsResponse() {
        when(cache.get(anyString(), eq(Object.class))).thenReturn("SAVED_RESPONSE");

        Object response = idempotencyService.getCachedResponse("key1", "payload");

        assertEquals("SAVED_RESPONSE", response);
    }

    @Test
    void getCachedResponse_notFound_returnsNull() {
        when(cache.get(anyString(), eq(Object.class))).thenReturn(null);

        Object response = idempotencyService.getCachedResponse("key1", "payload");

        assertNull(response);
    }

    @Test
    void save_putsIntoCache() {
        idempotencyService.save("key1", "payload", "result");

        verify(cache, times(1)).put(anyString(), eq("result"));
    }

    @Test
    void remove_evictsFromCache() {
        idempotencyService.remove("key1", "payload");

        verify(cache, times(1)).evict(anyString());
    }
}
