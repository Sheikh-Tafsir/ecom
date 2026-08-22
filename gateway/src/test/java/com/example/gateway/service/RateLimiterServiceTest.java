package com.example.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService(Optional.empty());

        ReflectionTestUtils.setField(rateLimiterService, "globalRateLimit", 2);
        ReflectionTestUtils.setField(rateLimiterService, "globalConcurrencyLimit", 2);
        ReflectionTestUtils.setField(rateLimiterService, "ipRateLimit", 2);
        ReflectionTestUtils.setField(rateLimiterService, "ipConcurrencyLimit", 2);
        ReflectionTestUtils.setField(rateLimiterService, "authIpRateLimit", 1);
        ReflectionTestUtils.setField(rateLimiterService, "authIpConcurrencyLimit", 1);
        ReflectionTestUtils.setField(rateLimiterService, "userRateLimit", 2);
        ReflectionTestUtils.setField(rateLimiterService, "userConcurrencyLimit", 2);
        ReflectionTestUtils.setField(rateLimiterService, "windowSeconds", 60);
    }

    @Test
    void testGlobalRateLimit() {
        assertTrue(rateLimiterService.isGlobalRateAllowed().block());
        assertTrue(rateLimiterService.isGlobalRateAllowed().block());
        assertFalse(rateLimiterService.isGlobalRateAllowed().block());
    }

    @Test
    void testIpRateLimit() {
        String ip = "192.168.1.10";
        assertTrue(rateLimiterService.isIpRateAllowed(ip, false).block());
        assertTrue(rateLimiterService.isIpRateAllowed(ip, false).block());
        assertFalse(rateLimiterService.isIpRateAllowed(ip, false).block());
    }

    @Test
    void testAuthIpRateLimit() {
        String ip = "192.168.1.20";
        assertTrue(rateLimiterService.isIpRateAllowed(ip, true).block());
        assertFalse(rateLimiterService.isIpRateAllowed(ip, true).block());
    }

    @Test
    void testUserRateLimit() {
        String email = "test@example.com";
        assertTrue(rateLimiterService.isUserRateAllowed(email).block());
        assertTrue(rateLimiterService.isUserRateAllowed(email).block());
        assertFalse(rateLimiterService.isUserRateAllowed(email).block());
    }

    @Test
    void testConcurrencyAcquireAndRelease() {
        assertTrue(rateLimiterService.acquireGlobalConcurrency().block());
        assertTrue(rateLimiterService.acquireGlobalConcurrency().block());
        assertFalse(rateLimiterService.acquireGlobalConcurrency().block());

        rateLimiterService.releaseGlobalConcurrency().block();
        assertTrue(rateLimiterService.acquireGlobalConcurrency().block());
    }

    @Test
    void testShouldBypassRateLimit() {
        MockServerWebExchange socketExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/socket.io/1/").build()
        );
        assertTrue(RateLimiterService.shouldBypassRateLimit(socketExchange));

        MockServerWebExchange actuatorExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build()
        );
        assertTrue(RateLimiterService.shouldBypassRateLimit(actuatorExchange));

        MockServerWebExchange normalExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/products").build()
        );
        assertFalse(RateLimiterService.shouldBypassRateLimit(normalExchange));
    }
}
