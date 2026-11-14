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
class IpTrafficControlFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private IpTrafficControlFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/products");
        request.setRemoteAddr("192.168.1.100");
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Allows request when IP rate and concurrency are within limits")
    void doFilter_withinLimits_proceedsFilterChain() throws ServletException, IOException {
        when(rateLimiterService.isIpRateAllowed("192.168.1.100", false)).thenReturn(true);
        when(rateLimiterService.acquireIpConcurrency("192.168.1.100", false)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService).releaseIpConcurrency("192.168.1.100", false);
    }

    @Test
    @DisplayName("Uses auth-path rate limits for /auth endpoints")
    void doFilter_authPath_usesAuthLimits() throws ServletException, IOException {
        request.setRequestURI("/auth/login");
        when(rateLimiterService.isIpRateAllowed("192.168.1.100", true)).thenReturn(true);
        when(rateLimiterService.acquireIpConcurrency("192.168.1.100", true)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService).releaseIpConcurrency("192.168.1.100", true);
    }

    @Test
    @DisplayName("Rejects request with 429 when IP rate limit exceeded")
    void doFilter_ipRateLimitExceeded_returns429() throws ServletException, IOException {
        when(rateLimiterService.isIpRateAllowed("192.168.1.100", false)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getStatus());
    }

    @Test
    @DisplayName("Rejects request with 429 when IP concurrency limit exceeded")
    void doFilter_ipConcurrencyExceeded_returns429() throws ServletException, IOException {
        when(rateLimiterService.isIpRateAllowed("192.168.1.100", false)).thenReturn(true);
        when(rateLimiterService.acquireIpConcurrency("192.168.1.100", false)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getStatus());
    }

    @Test
    @DisplayName("Rejects request with 400 when client IP is missing")
    void doFilter_missingIp_returns400() throws ServletException, IOException {
        // Use a fresh request without setting remoteAddr headers;
        // spy it so getRemoteAddr() returns null without passing null to setRemoteAddr
        MockHttpServletRequest noIpRequest = spy(new MockHttpServletRequest());
        noIpRequest.setMethod("GET");
        noIpRequest.setRequestURI("/api/products");
        doReturn(null).when(noIpRequest).getRemoteAddr();

        filter.doFilterInternal(noIpRequest, response, filterChain);

        verify(filterChain, never()).doFilter(noIpRequest, response);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    @Test
    @DisplayName("Uses X-Real-IP header when present")
    void doFilter_xRealIpHeader_usesHeaderIp() throws ServletException, IOException {
        request.addHeader("X-Real-IP", "10.0.0.1");
        when(rateLimiterService.isIpRateAllowed("10.0.0.1", false)).thenReturn(true);
        when(rateLimiterService.acquireIpConcurrency("10.0.0.1", false)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService).releaseIpConcurrency("10.0.0.1", false);
    }

    @Test
    @DisplayName("Releases IP concurrency even when filter chain throws exception")
    void doFilter_filterChainThrows_releasesIpConcurrency() throws ServletException, IOException {
        when(rateLimiterService.isIpRateAllowed("192.168.1.100", false)).thenReturn(true);
        when(rateLimiterService.acquireIpConcurrency("192.168.1.100", false)).thenReturn(true);
        doThrow(new ServletException("downstream failure")).when(filterChain).doFilter(request, response);

        assertThrows(ServletException.class, () -> filter.doFilterInternal(request, response, filterChain));

        verify(rateLimiterService).releaseIpConcurrency("192.168.1.100", false);
    }
}
