package com.example.ecom.common.filter;

import com.example.ecom.common.service.JwtService;
import com.example.ecom.notification.service.NotificationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.example.ecom.common.utils.CacheConstants.CACHE_REVOKED_ACCESS_TOKENS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache revokedTokensCache;

    @Mock
    private NotificationService notificationService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    @InjectMocks
    private AuthenticationFilter authenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Valid active token should authenticate user and proceed filter chain")
    void doFilter_whenTokenValidAndNotRevoked_authenticatesUser() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.parseAccessTokenClaims("valid-token")).thenReturn(claims);
        when(claims.getId()).thenReturn("jti-123");
        when(claims.getSubject()).thenReturn("42");
        when(claims.get("email", String.class)).thenReturn("user@example.com");
        when(claims.get("status", String.class)).thenReturn("ACTIVE");
        when(claims.get("permissions", List.class)).thenReturn(List.of("READ"));

        when(cacheManager.getCache(CACHE_REVOKED_ACCESS_TOKENS)).thenReturn(revokedTokensCache);
        when(revokedTokensCache.get("jti-123")).thenReturn(null);
        when(revokedTokensCache.get("user:42")).thenReturn(null);

        authenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("42", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    @DisplayName("Token with blacklisted JTI should be rejected with 401")
    void doFilter_whenTokenJtiRevoked_rejectsWith401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer revoked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.parseAccessTokenClaims("revoked-token")).thenReturn(claims);
        when(claims.getId()).thenReturn("jti-revoked");

        Cache.ValueWrapper blacklistedValue = mock(Cache.ValueWrapper.class);
        when(blacklistedValue.get()).thenReturn(true);

        when(cacheManager.getCache(CACHE_REVOKED_ACCESS_TOKENS)).thenReturn(revokedTokensCache);
        when(revokedTokensCache.get("jti-revoked")).thenReturn(blacklistedValue);

        authenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("Access token has been revoked"));
    }

    @Test
    @DisplayName("Token issued prior to user revocation timestamp (ban/password reset) should be rejected with 401")
    void doFilter_whenUserRevokedAfterTokenIssued_rejectsWith401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer old-user-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        long tokenIssuedAtMilli = 1_000_000_000L;
        long userRevokedAtMilli = 1_000_005_000L; // Revoked 5 seconds AFTER token was issued

        when(jwtService.parseAccessTokenClaims("old-user-token")).thenReturn(claims);
        when(claims.getId()).thenReturn("jti-active");
        when(claims.getSubject()).thenReturn("99");
        when(claims.getIssuedAt()).thenReturn(new Date(tokenIssuedAtMilli));

        Cache.ValueWrapper userRevocationWrapper = mock(Cache.ValueWrapper.class);
        when(userRevocationWrapper.get()).thenReturn(userRevokedAtMilli);

        when(cacheManager.getCache(CACHE_REVOKED_ACCESS_TOKENS)).thenReturn(revokedTokensCache);
        when(revokedTokensCache.get("jti-active")).thenReturn(null);
        when(revokedTokensCache.get("user:99")).thenReturn(userRevocationWrapper);

        authenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("Session has been revoked"));
    }

    @Test
    @DisplayName("Token issued after user revocation timestamp should be accepted")
    void doFilter_whenUserRevokedBeforeTokenIssued_permitsRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/profile");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer fresh-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        long userRevokedAtMilli = 1_000_000_000L;
        long tokenIssuedAtMilli = 1_000_010_000L; // Fresh token issued 10 seconds AFTER password reset / unban

        when(jwtService.parseAccessTokenClaims("fresh-token")).thenReturn(claims);
        when(claims.getId()).thenReturn("jti-fresh");
        when(claims.getSubject()).thenReturn("99");
        when(claims.getIssuedAt()).thenReturn(new Date(tokenIssuedAtMilli));
        when(claims.get("email", String.class)).thenReturn("user@example.com");
        when(claims.get("status", String.class)).thenReturn("ACTIVE");
        when(claims.get("permissions", List.class)).thenReturn(List.of("READ"));

        Cache.ValueWrapper userRevocationWrapper = mock(Cache.ValueWrapper.class);
        when(userRevocationWrapper.get()).thenReturn(userRevokedAtMilli);

        when(cacheManager.getCache(CACHE_REVOKED_ACCESS_TOKENS)).thenReturn(revokedTokensCache);
        when(revokedTokensCache.get("jti-fresh")).thenReturn(null);
        when(revokedTokensCache.get("user:99")).thenReturn(userRevocationWrapper);

        authenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Logout request with revoked token should still proceed to filter chain so refresh token is revoked")
    void doFilter_whenLogoutRequestWithRevokedToken_allowsFilterChainToProceed() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/logout");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer revoked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.parseAccessTokenClaims("revoked-token")).thenReturn(claims);
        when(claims.getId()).thenReturn("jti-revoked");

        Cache.ValueWrapper blacklistedValue = mock(Cache.ValueWrapper.class);
        when(blacklistedValue.get()).thenReturn(true);

        when(cacheManager.getCache(CACHE_REVOKED_ACCESS_TOKENS)).thenReturn(revokedTokensCache);
        when(revokedTokensCache.get("jti-revoked")).thenReturn(blacklistedValue);

        authenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
