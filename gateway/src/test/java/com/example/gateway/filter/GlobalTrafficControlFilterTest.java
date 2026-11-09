package com.example.gateway.filter;

import com.example.gateway.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalTrafficControlFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private WebFilterChain chain;

    private GlobalTrafficControlFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GlobalTrafficControlFilter(rateLimiterService);
    }

    @Test
    @DisplayName("filter bypasses rate limiting for socket.io paths")
    void filter_socketIoPath_bypassesRateLimit() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/socket.io/connect").build()
        );
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(rateLimiterService, never()).isGlobalRateAllowed();
    }

    @Test
    @DisplayName("filter bypasses rate limiting for actuator paths")
    void filter_actuatorPath_bypassesRateLimit() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build()
        );
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(rateLimiterService, never()).isGlobalRateAllowed();
    }

    @Test
    @DisplayName("filter allows request when global rate limit and concurrency are within limits")
    void filter_withinLimits_allowsRequest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products").build()
        );

        when(rateLimiterService.isGlobalRateAllowed()).thenReturn(Mono.just(true));
        when(rateLimiterService.acquireGlobalConcurrency()).thenReturn(Mono.just(true));
        when(rateLimiterService.releaseGlobalConcurrency()).thenReturn(Mono.empty());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(rateLimiterService).isGlobalRateAllowed();
        verify(rateLimiterService).acquireGlobalConcurrency();
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("filter returns 429 TOO_MANY_REQUESTS when global rate limit is exceeded")
    void filter_globalRateLimitExceeded_returns429() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products").build()
        );

        when(rateLimiterService.isGlobalRateAllowed()).thenReturn(Mono.just(false));

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
        verify(rateLimiterService).isGlobalRateAllowed();
        verify(rateLimiterService, never()).acquireGlobalConcurrency();
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("filter returns 503 SERVICE_UNAVAILABLE when global concurrency limit is exceeded")
    void filter_globalConcurrencyLimitExceeded_returns503() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products").build()
        );

        when(rateLimiterService.isGlobalRateAllowed()).thenReturn(Mono.just(true));
        when(rateLimiterService.acquireGlobalConcurrency()).thenReturn(Mono.just(false));

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
        verify(rateLimiterService).acquireGlobalConcurrency();
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("filter releases global concurrency after successful chain processing")
    void filter_successfulRequest_releasesConcurrency() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products").build()
        );

        when(rateLimiterService.isGlobalRateAllowed()).thenReturn(Mono.just(true));
        when(rateLimiterService.acquireGlobalConcurrency()).thenReturn(Mono.just(true));
        when(rateLimiterService.releaseGlobalConcurrency()).thenReturn(Mono.empty());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(rateLimiterService).releaseGlobalConcurrency();
    }

    @Test
    @DisplayName("filter releases global concurrency when chain processing errors")
    void filter_chainError_releasesConcurrency() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products").build()
        );

        when(rateLimiterService.isGlobalRateAllowed()).thenReturn(Mono.just(true));
        when(rateLimiterService.acquireGlobalConcurrency()).thenReturn(Mono.just(true));
        when(rateLimiterService.releaseGlobalConcurrency()).thenReturn(Mono.empty());
        when(chain.filter(exchange)).thenReturn(Mono.error(new RuntimeException("downstream error")));

        try {
            filter.filter(exchange, chain).block();
        } catch (RuntimeException ignored) {
            // expected
        }

        verify(rateLimiterService).releaseGlobalConcurrency();
    }

    @Test
    @DisplayName("filter bypasses rate limiting for swagger-ui paths")
    void filter_swaggerUiPath_bypassesRateLimit() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/swagger-ui/index.html").build()
        );
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(rateLimiterService, never()).isGlobalRateAllowed();
    }

    @Test
    @DisplayName("filter bypasses rate limiting for API docs paths")
    void filter_apiDocsPath_bypassesRateLimit() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/v3/api-docs").build()
        );
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(rateLimiterService, never()).isGlobalRateAllowed();
    }

    @Test
    @DisplayName("filter applies rate limiting for normal API paths")
    void filter_normalApiPath_appliesRateLimit() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/orders").build()
        );

        when(rateLimiterService.isGlobalRateAllowed()).thenReturn(Mono.just(true));
        when(rateLimiterService.acquireGlobalConcurrency()).thenReturn(Mono.just(true));
        when(rateLimiterService.releaseGlobalConcurrency()).thenReturn(Mono.empty());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(rateLimiterService).isGlobalRateAllowed();
        verify(rateLimiterService).acquireGlobalConcurrency();
    }
}
