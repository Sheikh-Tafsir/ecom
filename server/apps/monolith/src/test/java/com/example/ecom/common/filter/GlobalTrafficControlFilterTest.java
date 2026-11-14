package com.example.ecom.common.filter;

import com.example.ecom.common.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalTrafficControlFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private GlobalTrafficControlFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/products");
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Allows request when both global rate and concurrency are within limits")
    void doFilter_withinLimits_proceedsFilterChain() throws ServletException, IOException {
        when(rateLimiterService.isGlobalRateAllowed()).thenReturn(true);
        when(rateLimiterService.acquireGlobalConcurrency()).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService).releaseGlobalConcurrency();
    }

    @Test
    @DisplayName("Rejects request with 429 when global rate limit exceeded")
    void doFilter_rateLimitExceeded_returns429() throws ServletException, IOException {
        when(rateLimiterService.isGlobalRateAllowed()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getStatus());
        verify(rateLimiterService, never()).acquireGlobalConcurrency();
    }

    @Test
    @DisplayName("Rejects request with 503 when global concurrency limit exceeded")
    void doFilter_concurrencyLimitExceeded_returns503() throws ServletException, IOException {
        when(rateLimiterService.isGlobalRateAllowed()).thenReturn(true);
        when(rateLimiterService.acquireGlobalConcurrency()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getStatus());
    }

    @Test
    @DisplayName("Releases global concurrency even when filter chain throws exception")
    void doFilter_filterChainThrows_releasesGlobalConcurrency() throws ServletException, IOException {
        when(rateLimiterService.isGlobalRateAllowed()).thenReturn(true);
        when(rateLimiterService.acquireGlobalConcurrency()).thenReturn(true);
        doThrow(new ServletException("downstream failure")).when(filterChain).doFilter(request, response);

        assertThrows(ServletException.class, () -> filter.doFilterInternal(request, response, filterChain));

        verify(rateLimiterService).releaseGlobalConcurrency();
    }
}
