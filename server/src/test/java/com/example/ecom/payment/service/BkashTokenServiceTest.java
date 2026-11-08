package com.example.ecom.payment.service;

import com.example.ecom.common.config.BkashConfigProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.reactive.function.client.WebClient;

import static com.example.ecom.common.utils.CacheConstants.CACHE_BKASH_ID_TOKEN;
import static com.example.ecom.common.utils.CacheConstants.CACHE_BKASH_REFRESH_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BkashTokenServiceTest {

    @Mock
    private BkashConfigProperties config;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache idTokenCache;

    @Mock
    private Cache refreshTokenCache;

    @Mock
    private WebClient webClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BkashTokenService bkashTokenService;

    @BeforeEach
    void setUp() {
        bkashTokenService = new BkashTokenService(config, cacheManager, webClient, objectMapper);
    }

    @Test
    void getValidToken_whenIdTokenCached_returnsCachedTokenWithoutHttpCall() {
        when(cacheManager.getCache(CACHE_BKASH_ID_TOKEN)).thenReturn(idTokenCache);
        when(idTokenCache.get("token", String.class)).thenReturn("cached-valid-id-token");

        String token = bkashTokenService.getValidToken();

        assertEquals("cached-valid-id-token", token);
        verifyNoInteractions(webClient);
    }

    @Test
    void getValidToken_whenIdTokenNotCached_checksRefreshToken() {
        when(cacheManager.getCache(CACHE_BKASH_ID_TOKEN)).thenReturn(idTokenCache);
        when(idTokenCache.get("token", String.class)).thenReturn(null);

        when(cacheManager.getCache(CACHE_BKASH_REFRESH_TOKEN)).thenReturn(refreshTokenCache);
        when(refreshTokenCache.get("token", String.class)).thenReturn(null);

        // Both caches empty — will attempt grantToken which calls bkashWebClient
        // We just verify it checked both caches before resorting to HTTP
        try {
            bkashTokenService.getValidToken();
        } catch (Exception ignored) {
            // WebClient is not stubbed, will throw — that's expected
        }

        verify(cacheManager).getCache(CACHE_BKASH_ID_TOKEN);
        verify(cacheManager).getCache(CACHE_BKASH_REFRESH_TOKEN);
    }
}
