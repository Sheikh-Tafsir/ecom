package com.example.ecom.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingFilterTest {

    @InjectMocks
    private LoggingFilter loggingFilter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Sets X-Request-Id header from incoming request when present")
    void doFilter_withRequestIdHeader_usesExistingId() throws ServletException, IOException {
        request.addHeader(LoggingFilter.REQUEST_ID_HEADER, "req-abc-123");
        request.setMethod("GET");
        request.setRequestURI("/api/products");

        loggingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals("req-abc-123", response.getHeader(LoggingFilter.REQUEST_ID_HEADER));
    }

    @Test
    @DisplayName("Generates a new X-Request-Id when not provided in request")
    void doFilter_withoutRequestIdHeader_generatesNewId() throws ServletException, IOException {
        request.setMethod("POST");
        request.setRequestURI("/api/orders");

        loggingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        String requestId = response.getHeader(LoggingFilter.REQUEST_ID_HEADER);
        assertNotNull(requestId);
        assertFalse(requestId.isBlank());
    }

    @Test
    @DisplayName("Proceeds with the filter chain on normal requests")
    void doFilter_normalRequest_proceedsFilterChain() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI("/api/categories");
        request.setQueryString("page=0&size=10");

        loggingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Redacts sensitive query parameters like token and password")
    void doFilter_sensitiveQueryParams_redacted() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI("/api/auth/callback");
        request.setQueryString("accessToken=secret123&name=test");

        loggingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Handles null query string without errors")
    void doFilter_nullQueryString_noError() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI("/api/products");

        loggingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
