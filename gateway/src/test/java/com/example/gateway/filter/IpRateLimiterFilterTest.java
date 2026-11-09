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

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IpRateLimiterFilterTest {

    private static final String CLIENT_IP = "203.0.113.45";

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private WebFilterChain chain;

    private IpRateLimiterFilter filter;

    @BeforeEach
    void setUp() {
        filter = new IpRateLimiterFilter(rateLimiterService, "");
    }

    @Test
    @DisplayName("filter bypasses rate limiting for socket.io paths")
    void filter_socketIoPath_bypassesRateLimit() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/socket.io/1/").build()
        );
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(rateLimiterService, never()).isIpRateAllowed(anyString(), anyBoolean());
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
    }

    @Test
    @DisplayName("filter allows request when IP rate limit and concurrency are within limits")
    void filter_withinLimits_allowsRequest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products")
                        .remoteAddress(new InetSocketAddress(CLIENT_IP, 12345))
                        .build()
        );

        when(rateLimiterService.isIpRateAllowed(CLIENT_IP, false)).thenReturn(Mono.just(true));
        when(rateLimiterService.acquireIpConcurrency(CLIENT_IP, false)).thenReturn(Mono.just(true));
        when(rateLimiterService.releaseIpConcurrency(CLIENT_IP, false)).thenReturn(Mono.empty());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(rateLimiterService).isIpRateAllowed(CLIENT_IP, false);
        verify(rateLimiterService).acquireIpConcurrency(CLIENT_IP, false);
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("filter returns 429 when IP rate limit is exceeded")
    void filter_rateLimitExceeded_returns429() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products")
                        .remoteAddress(new InetSocketAddress(CLIENT_IP, 12345))
                        .build()
        );

        when(rateLimiterService.isIpRateAllowed(CLIENT_IP, false)).thenReturn(Mono.just(false));

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
        verify(rateLimiterService).isIpRateAllowed(CLIENT_IP, false);
        verify(rateLimiterService, never()).acquireIpConcurrency(anyString(), anyBoolean());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("filter returns 429 when IP concurrency limit is exceeded")
    void filter_concurrencyLimitExceeded_returns429() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products")
                        .remoteAddress(new InetSocketAddress(CLIENT_IP, 12345))
                        .build()
        );

        when(rateLimiterService.isIpRateAllowed(CLIENT_IP, false)).thenReturn(Mono.just(true));
        when(rateLimiterService.acquireIpConcurrency(CLIENT_IP, false)).thenReturn(Mono.just(false));

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
        verify(rateLimiterService).acquireIpConcurrency(CLIENT_IP, false);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("filter identifies auth paths and uses auth rate limiting")
    void filter_authPath_usesAuthRateLimiting() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login")
                        .remoteAddress(new InetSocketAddress(CLIENT_IP, 12345))
                        .build()
        );

        when(rateLimiterService.isIpRateAllowed(CLIENT_IP, true)).thenReturn(Mono.just(true));
        when(rateLimiterService.acquireIpConcurrency(CLIENT_IP, true)).thenReturn(Mono.just(true));
        when(rateLimiterService.releaseIpConcurrency(CLIENT_IP, true)).thenReturn(Mono.empty());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(rateLimiterService).isIpRateAllowed(CLIENT_IP, true);
        verify(rateLimiterService).acquireIpConcurrency(CLIENT_IP, true);
    }

    @Test
    @DisplayName("filter uses non-auth rate limiting for non-auth paths")
    void filter_nonAuthPath_usesNonAuthRateLimiting() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/categories")
                        .remoteAddress(new InetSocketAddress(CLIENT_IP, 12345))
                        .build()
        );

        when(rateLimiterService.isIpRateAllowed(CLIENT_IP, false)).thenReturn(Mono.just(true));
        when(rateLimiterService.acquireIpConcurrency(CLIENT_IP, false)).thenReturn(Mono.just(true));
        when(rateLimiterService.releaseIpConcurrency(CLIENT_IP, false)).thenReturn(Mono.empty());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(rateLimiterService).isIpRateAllowed(CLIENT_IP, false);
    }

    // --- extractIp tests ---

    @Test
    @DisplayName("extractIp returns remote address host when no trusted proxy is configured")
    void extractIp_noTrustedProxy_returnsRemoteAddress() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products")
                .remoteAddress(new InetSocketAddress(CLIENT_IP, 12345))
                .build();

        String ip = filter.extractIp(request);

        assertEquals(CLIENT_IP, ip);
    }

    @Test
    @DisplayName("extractIp returns X-Forwarded-For first IP when request comes from loopback (trusted)")
    void extractIp_loopbackWithXForwardedFor_returnsForwardedIp() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 80))
                .header("X-Forwarded-For", "10.20.30.40, 172.16.0.1")
                .build();

        String ip = filter.extractIp(request);

        assertEquals("10.20.30.40", ip);
    }

    @Test
    @DisplayName("extractIp returns X-Real-IP when request comes from loopback and no X-Forwarded-For")
    void extractIp_loopbackWithXRealIp_returnsRealIp() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 80))
                .header("X-Real-IP", "192.168.1.100")
                .build();

        String ip = filter.extractIp(request);

        assertEquals("192.168.1.100", ip);
    }

    @Test
    @DisplayName("extractIp returns remote address when untrusted proxy sends X-Forwarded-For")
    void extractIp_untrustedProxyWithForwardedFor_returnsRemoteAddress() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products")
                .remoteAddress(new InetSocketAddress(CLIENT_IP, 12345))
                .header("X-Forwarded-For", "10.0.0.1, 172.16.0.1")
                .build();

        String ip = filter.extractIp(request);

        assertEquals(CLIENT_IP, ip);
    }

    @Test
    @DisplayName("extractIp uses X-Forwarded-For when remote address is a configured trusted proxy")
    void extractIp_configuredTrustedProxy_returnsForwardedIp() {
        IpRateLimiterFilter filterWithTrustedProxy = new IpRateLimiterFilter(rateLimiterService, "10.0.0.1");

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products")
                .remoteAddress(new InetSocketAddress("10.0.0.1", 80))
                .header("X-Forwarded-For", "55.66.77.88, 10.0.0.1")
                .build();

        String ip = filterWithTrustedProxy.extractIp(request);

        assertEquals("55.66.77.88", ip);
    }

    @Test
    @DisplayName("Constructor handles comma-separated trusted proxies with whitespace")
    void constructor_commaSeparatedTrustedProxies_parsesCorrectly() {
        IpRateLimiterFilter filterWithProxies = new IpRateLimiterFilter(
                rateLimiterService, "10.0.0.1, 10.0.0.2, 10.0.0.3"
        );

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products")
                .remoteAddress(new InetSocketAddress("10.0.0.2", 80))
                .header("X-Forwarded-For", "99.88.77.66")
                .build();

        String ip = filterWithProxies.extractIp(request);

        assertEquals("99.88.77.66", ip);
    }

    @Test
    @DisplayName("Constructor handles null trusted proxies config")
    void constructor_nullTrustedProxies_createsEmptySet() {
        IpRateLimiterFilter filterWithNull = new IpRateLimiterFilter(rateLimiterService, null);
        assertNotNull(filterWithNull);
    }

    @Test
    @DisplayName("Constructor handles blank trusted proxies config")
    void constructor_blankTrustedProxies_createsEmptySet() {
        IpRateLimiterFilter filterWithBlank = new IpRateLimiterFilter(rateLimiterService, "   ");
        assertNotNull(filterWithBlank);
    }

    @Test
    @DisplayName("extractIp returns loopback address when loopback is remote and no forwarded headers")
    void extractIp_loopbackNoForwardedHeaders_returnsLoopbackAddress() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 80))
                .build();

        String ip = filter.extractIp(request);

        assertEquals("127.0.0.1", ip);
    }
}
