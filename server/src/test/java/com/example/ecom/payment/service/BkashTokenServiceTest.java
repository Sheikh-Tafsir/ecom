package com.example.ecom.payment.service;

import com.example.ecom.common.config.BkashConfigProperties;
import com.example.ecom.common.service.cache.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BkashTokenServiceTest {

    @Mock
    private BkashConfigProperties config;

    @Mock
    private CacheService tokenCacheService;

    @Mock
    private WebClient webClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BkashTokenService bkashTokenService;

    @BeforeEach
    void setUp() {
        bkashTokenService = new BkashTokenService(config, tokenCacheService, webClient, objectMapper);
    }

    @Test
    void getValidToken_whenTokenCached_returnsCachedTokenWithoutHttpCall() {
        when(tokenCacheService.getToken("bkash:id_token")).thenReturn("cached-valid-id-token");

        String token = bkashTokenService.getValidToken();

        assertEquals("cached-valid-id-token", token);
        verify(tokenCacheService).getToken("bkash:id_token");
        verifyNoInteractions(webClient);
    }
}
