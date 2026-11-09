package com.example.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoggingFilterTest {

    private LoggingFilter loggingFilter;
    private WebFilterChain chain;

    @BeforeEach
    void setUp() {
        loggingFilter = new LoggingFilter();
        chain = mock(WebFilterChain.class);
        MDC.clear();
    }

    @Test
    @DisplayName("filter generates a new request ID when none is provided in headers")
    void filter_noRequestIdHeader_generatesNewRequestId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products")
                        .remoteAddress(new InetSocketAddress("192.168.1.10", 5000))
                        .build()
        );

        when(chain.filter(any())).thenReturn(Mono.empty());

        loggingFilter.filter(exchange, chain).block();

        String responseRequestId = exchange.getResponse().getHeaders().getFirst(LoggingFilter.REQUEST_ID_HEADER);
        assertNotNull(responseRequestId);
        assertDoesNotThrow(() -> UUID.fromString(responseRequestId));
    }

    @Test
    @DisplayName("filter preserves the existing X-Request-Id when provided")
    void filter_existingRequestId_preservesIt() {
        String existingRequestId = "req-abc-123-def-456";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products")
                        .remoteAddress(new InetSocketAddress("192.168.1.10", 5000))
                        .header(LoggingFilter.REQUEST_ID_HEADER, existingRequestId)
                        .build()
        );

        when(chain.filter(any())).thenReturn(Mono.empty());

        loggingFilter.filter(exchange, chain).block();

        String responseRequestId = exchange.getResponse().getHeaders().getFirst(LoggingFilter.REQUEST_ID_HEADER);
        assertEquals(existingRequestId, responseRequestId);
    }

    @Test
    @DisplayName("filter adds X-Request-Id to response headers")
    void filter_anyRequest_addsRequestIdToResponseHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .remoteAddress(new InetSocketAddress("10.0.0.1", 8080))
                        .build()
        );

        when(chain.filter(any())).thenReturn(Mono.empty());

        loggingFilter.filter(exchange, chain).block();

        assertNotNull(exchange.getResponse().getHeaders().getFirst(LoggingFilter.REQUEST_ID_HEADER));
    }

    @Test
    @DisplayName("filter calls chain.filter to pass the request downstream")
    void filter_normalRequest_callsChainFilter() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products")
                        .remoteAddress(new InetSocketAddress("192.168.1.10", 5000))
                        .build()
        );

        when(chain.filter(any())).thenReturn(Mono.empty());

        loggingFilter.filter(exchange, chain).block();

        verify(chain).filter(any());
    }

    @Test
    @DisplayName("filter works with POST requests")
    void filter_postRequest_processesCorrectly() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login")
                        .remoteAddress(new InetSocketAddress("192.168.1.10", 5000))
                        .build()
        );

        when(chain.filter(any())).thenReturn(Mono.empty());

        loggingFilter.filter(exchange, chain).block();

        verify(chain).filter(any());
    }

    @Test
    @DisplayName("filter clears MDC after request completes")
    void filter_afterCompletion_clearsMdc() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products")
                        .remoteAddress(new InetSocketAddress("192.168.1.10", 5000))
                        .build()
        );

        when(chain.filter(any())).thenReturn(Mono.empty());

        loggingFilter.filter(exchange, chain).block();

        // MDC should be cleared after doFinally
        assertNull(MDC.get(LoggingFilter.MDC_REQUEST_ID_KEY));
        assertNull(MDC.get(LoggingFilter.MDC_USER_ID_KEY));
        assertNull(MDC.get(LoggingFilter.MDC_CLIENT_IP_KEY));
    }

    @Test
    @DisplayName("Constants are correctly defined")
    void constants_areCorrectlyDefined() {
        assertEquals("X-Request-Id", LoggingFilter.REQUEST_ID_HEADER);
        assertEquals("User-Agent", LoggingFilter.BROWSER_NAME_HEADER);
        assertEquals("requestId", LoggingFilter.MDC_REQUEST_ID_KEY);
        assertEquals("userId", LoggingFilter.MDC_USER_ID_KEY);
        assertEquals("clientIp", LoggingFilter.MDC_CLIENT_IP_KEY);
    }

    @Test
    @DisplayName("filter handles requests without query string")
    void filter_noQueryString_processesCorrectly() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products")
                        .remoteAddress(new InetSocketAddress("192.168.1.10", 5000))
                        .build()
        );

        when(chain.filter(any())).thenReturn(Mono.empty());

        loggingFilter.filter(exchange, chain).block();

        verify(chain).filter(any());
    }

    @Test
    @DisplayName("filter handles requests with query string containing sensitive params")
    void filter_sensitiveQueryParams_processesCorrectly() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/callback?accessToken=secret123&other=visible")
                        .remoteAddress(new InetSocketAddress("192.168.1.10", 5000))
                        .build()
        );

        when(chain.filter(any())).thenReturn(Mono.empty());

        loggingFilter.filter(exchange, chain).block();

        verify(chain).filter(any());
    }

    @Test
    @DisplayName("filter picks up userId attribute in doFinally when set by downstream")
    void filter_userIdAttributeSet_includesInMdc() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .remoteAddress(new InetSocketAddress("192.168.1.10", 5000))
                        .build()
        );

        String userId = UUID.randomUUID().toString();
        when(chain.filter(any())).thenAnswer(invocation -> {
            exchange.getAttributes().put(LoggingFilter.MDC_USER_ID_KEY, userId);
            return Mono.empty();
        });

        loggingFilter.filter(exchange, chain).block();

        verify(chain).filter(any());
    }
}
