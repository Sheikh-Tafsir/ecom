package com.example.gateway.filter;

import com.example.gateway.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    private static final String REVOKED_PREFIX = "auth:revoked:";
    private static final String USER_ID = "c1d2e3f4-5a6b-7c8d-9e0f-1a2b3c4d5e6f";
    private static final String USER_EMAIL = "testuser@example.com";

    @Mock
    private JwtService jwtService;

    @Mock
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Mock
    private WebFilterChain chain;

    private AuthenticationFilter authenticationFilter;

    @BeforeEach
    void setUp() {
        authenticationFilter = new AuthenticationFilter(jwtService, reactiveStringRedisTemplate, REVOKED_PREFIX);
    }

    private Claims mockClaims(String jti, String subject, String email, String status, List<String> permissions) {
        Claims claims = mock(Claims.class);
        lenient().when(claims.getId()).thenReturn(jti);
        lenient().when(claims.getSubject()).thenReturn(subject);
        lenient().when(claims.get("email", String.class)).thenReturn(email);
        lenient().when(claims.get("status", String.class)).thenReturn(status);
        lenient().when(claims.get("permissions", List.class)).thenReturn(permissions);
        return claims;
    }

    @Test
    @DisplayName("filter passes through when no Authorization header is present")
    void filter_noAuthHeader_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products").build()
        );
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        authenticationFilter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(jwtService, never()).parseAccessTokenClaims(anyString());
    }

    @Test
    @DisplayName("filter passes through when Authorization header is empty")
    void filter_emptyAuthHeader_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "")
                        .build()
        );
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        authenticationFilter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("filter passes through when Authorization header is not Bearer type")
    void filter_nonBearerAuth_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                        .build()
        );
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        authenticationFilter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("filter returns 401 UNAUTHORIZED for an invalid/expired JWT token")
    void filter_invalidToken_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                        .build()
        );
        when(jwtService.parseAccessTokenClaims("invalid.jwt.token"))
                .thenThrow(new JwtException("Token is expired"));

        authenticationFilter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(jwtService).parseAccessTokenClaims("invalid.jwt.token");
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("filter returns 401 UNAUTHORIZED when token JTI is blacklisted in Redis")
    void filter_blacklistedToken_returns401() {
        String jti = UUID.randomUUID().toString();
        Claims claims = mockClaims(jti, USER_ID, USER_EMAIL, "Active", List.of("ROLE_USER"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token.here")
                        .build()
        );

        when(jwtService.parseAccessTokenClaims("valid.token.here")).thenReturn(claims);
        when(reactiveStringRedisTemplate.hasKey(REVOKED_PREFIX + jti)).thenReturn(Mono.just(true));

        authenticationFilter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(reactiveStringRedisTemplate).hasKey(REVOKED_PREFIX + jti);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("filter authenticates user and calls chain when token is valid and not blacklisted")
    void filter_validNonBlacklistedToken_authenticatesAndProceedsWithChain() {
        String jti = UUID.randomUUID().toString();
        Claims claims = mockClaims(jti, USER_ID, USER_EMAIL, "Active", List.of("ROLE_USER"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token.here")
                        .build()
        );

        when(jwtService.parseAccessTokenClaims("valid.token.here")).thenReturn(claims);
        when(reactiveStringRedisTemplate.hasKey(REVOKED_PREFIX + jti)).thenReturn(Mono.just(false));
        when(chain.filter(any())).thenReturn(Mono.empty());

        authenticationFilter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertEquals(USER_ID, exchange.getAttribute(LoggingFilter.MDC_USER_ID_KEY));
    }

    @Test
    @DisplayName("filter returns 401 UNAUTHORIZED when user status is not ACTIVE")
    void filter_inactiveUser_returns401() {
        String jti = UUID.randomUUID().toString();
        Claims claims = mockClaims(jti, USER_ID, USER_EMAIL, "Suspended", List.of("ROLE_USER"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token.here")
                        .build()
        );

        when(jwtService.parseAccessTokenClaims("valid.token.here")).thenReturn(claims);
        when(reactiveStringRedisTemplate.hasKey(REVOKED_PREFIX + jti)).thenReturn(Mono.just(false));

        authenticationFilter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("filter returns 503 SERVICE_UNAVAILABLE when Redis is unreachable")
    void filter_redisError_returns503() {
        String jti = UUID.randomUUID().toString();
        Claims claims = mockClaims(jti, USER_ID, USER_EMAIL, "Active", List.of("ROLE_USER"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token.here")
                        .build()
        );

        when(jwtService.parseAccessTokenClaims("valid.token.here")).thenReturn(claims);
        when(reactiveStringRedisTemplate.hasKey(REVOKED_PREFIX + jti))
                .thenReturn(Mono.error(new RuntimeException("Redis connection refused")));

        authenticationFilter.filter(exchange, chain).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("filter uses local cache on second request for same non-blacklisted JTI")
    void filter_cachedNonBlacklistedJti_skipsRedis() {
        String jti = UUID.randomUUID().toString();
        Claims claims = mockClaims(jti, USER_ID, USER_EMAIL, "Active", List.of("ROLE_USER"));

        // First request — hits Redis
        MockServerWebExchange exchange1 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token.here")
                        .build()
        );
        when(jwtService.parseAccessTokenClaims("valid.token.here")).thenReturn(claims);
        when(reactiveStringRedisTemplate.hasKey(REVOKED_PREFIX + jti)).thenReturn(Mono.just(false));
        when(chain.filter(any())).thenReturn(Mono.empty());

        authenticationFilter.filter(exchange1, chain).block();
        verify(reactiveStringRedisTemplate, times(1)).hasKey(REVOKED_PREFIX + jti);

        // Second request — should use local cache, no Redis call
        MockServerWebExchange exchange2 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token.here")
                        .build()
        );

        authenticationFilter.filter(exchange2, chain).block();

        // Redis was still only called once (from the first request)
        verify(reactiveStringRedisTemplate, times(1)).hasKey(REVOKED_PREFIX + jti);
    }

    @Test
    @DisplayName("filter uses local cache and returns 401 for cached blacklisted JTI")
    void filter_cachedBlacklistedJti_returns401WithoutRedis() {
        String jti = UUID.randomUUID().toString();
        Claims claims = mockClaims(jti, USER_ID, USER_EMAIL, "Active", List.of("ROLE_USER"));

        // First request — hits Redis, token is blacklisted
        MockServerWebExchange exchange1 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token.here")
                        .build()
        );
        when(jwtService.parseAccessTokenClaims("valid.token.here")).thenReturn(claims);
        when(reactiveStringRedisTemplate.hasKey(REVOKED_PREFIX + jti)).thenReturn(Mono.just(true));

        authenticationFilter.filter(exchange1, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange1.getResponse().getStatusCode());
        verify(reactiveStringRedisTemplate, times(1)).hasKey(REVOKED_PREFIX + jti);

        // Second request — should use local cache
        MockServerWebExchange exchange2 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token.here")
                        .build()
        );

        authenticationFilter.filter(exchange2, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange2.getResponse().getStatusCode());
        // Redis was still only called once
        verify(reactiveStringRedisTemplate, times(1)).hasKey(REVOKED_PREFIX + jti);
    }

    @Test
    @DisplayName("filter returns 401 when user status is BANNED")
    void filter_bannedUser_returns401() {
        String jti = UUID.randomUUID().toString();
        Claims claims = mockClaims(jti, USER_ID, USER_EMAIL, "Banned", List.of("ROLE_USER"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer banned.user.token")
                        .build()
        );

        when(jwtService.parseAccessTokenClaims("banned.user.token")).thenReturn(claims);
        when(reactiveStringRedisTemplate.hasKey(REVOKED_PREFIX + jti)).thenReturn(Mono.just(false));

        authenticationFilter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("BEARER_PREFIX constant is 'Bearer '")
    void bearerPrefix_constant_isCorrect() {
        assertEquals("Bearer ", AuthenticationFilter.BEARER_PREFIX);
    }
}
