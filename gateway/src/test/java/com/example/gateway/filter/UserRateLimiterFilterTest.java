package com.example.gateway.filter;

import com.example.gateway.dto.CustomUserDetails;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRateLimiterFilterTest {

    private static final String USER_EMAIL = "jane.smith@example.com";

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private WebFilterChain chain;

    private UserRateLimiterFilter filter;

    @BeforeEach
    void setUp() {
        filter = new UserRateLimiterFilter(rateLimiterService);
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
        verify(rateLimiterService, never()).isUserRateAllowed(anyString());
    }

    @Test
    @DisplayName("filter passes through when no security context is present (unauthenticated)")
    void filter_noSecurityContext_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products").build()
        );
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(rateLimiterService, never()).isUserRateAllowed(anyString());
    }

    @Test
    @DisplayName("filter allows request when user rate limit and concurrency are within limits")
    void filter_authenticatedWithinLimits_allowsRequest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders").build()
        );

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getEmail()).thenReturn(USER_EMAIL);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(rateLimiterService.isUserRateAllowed(USER_EMAIL)).thenReturn(Mono.just(true));
        when(rateLimiterService.acquireUserConcurrency(USER_EMAIL)).thenReturn(Mono.just(true));
        when(rateLimiterService.releaseUserConcurrency(USER_EMAIL)).thenReturn(Mono.empty());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(new SecurityContextImpl(auth))
                ))
                .block();

        verify(rateLimiterService).isUserRateAllowed(USER_EMAIL);
        verify(rateLimiterService).acquireUserConcurrency(USER_EMAIL);
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("filter returns 429 when user rate limit is exceeded")
    void filter_userRateLimitExceeded_returns429() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders").build()
        );

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getEmail()).thenReturn(USER_EMAIL);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(rateLimiterService.isUserRateAllowed(USER_EMAIL)).thenReturn(Mono.just(false));

        filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(new SecurityContextImpl(auth))
                ))
                .block();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
        verify(rateLimiterService, never()).acquireUserConcurrency(anyString());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("filter returns 429 when user concurrency limit is exceeded")
    void filter_userConcurrencyLimitExceeded_returns429() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders").build()
        );

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getEmail()).thenReturn(USER_EMAIL);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(rateLimiterService.isUserRateAllowed(USER_EMAIL)).thenReturn(Mono.just(true));
        when(rateLimiterService.acquireUserConcurrency(USER_EMAIL)).thenReturn(Mono.just(false));

        filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                        Mono.just(new SecurityContextImpl(auth))
                ))
                .block();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
        verify(rateLimiterService).acquireUserConcurrency(USER_EMAIL);
        verify(chain, never()).filter(any());
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
        verify(rateLimiterService, never()).isUserRateAllowed(anyString());
    }
}
