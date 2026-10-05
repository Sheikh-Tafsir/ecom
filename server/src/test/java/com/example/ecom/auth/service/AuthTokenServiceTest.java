package com.example.ecom.auth.service;

import com.example.ecom.common.enums.UserRefreshTokenStatus;
import com.example.ecom.auth.repository.UserRefreshTokenRepository;
import com.example.ecom.common.service.JwtService;
import jakarta.servlet.http.Cookie;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static com.example.ecom.common.utils.CacheConstants.CACHE_REVOKED_ACCESS_TOKENS;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRefreshTokenRepository userRefreshTokenRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache revokedAccessTokensCache;

    @InjectMocks
    private AuthTokenService authTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authTokenService, "springProfile", "prod");
        ReflectionTestUtils.setField(authTokenService, "refreshTokenName", "refreshToken");
        ReflectionTestUtils.setField(authTokenService, "refreshTokenValidity", 604800L);
    }

    @Test
    @DisplayName("Should blacklist access token in cache when Authorization header is present during logout")
    void logout_whenBearerTokenPresent_blacklistsAccessJtiInCache() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.getJtiFromAccessToken("valid-access-token")).thenReturn("access-jti-999");
        when(cacheManager.getCache(CACHE_REVOKED_ACCESS_TOKENS)).thenReturn(revokedAccessTokensCache);

        authTokenService.logout(request, response);

        verify(revokedAccessTokensCache).put("access-jti-999", true);
        String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("refreshToken="));
        assertTrue(setCookieHeader.contains("Max-Age=0"));
    }

    @Test
    @DisplayName("Should revoke refresh token in database when valid refresh cookie is present during logout")
    void logout_whenRefreshTokenCookiePresent_revokesRefreshTokenRecord() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", "my-refresh-token-jwt"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        com.example.ecom.common.model.UserRefreshToken tokenRecord = new com.example.ecom.common.model.UserRefreshToken();
        tokenRecord.setJti("refresh-jti-888");
        tokenRecord.setStatus(UserRefreshTokenStatus.ACTIVE);

        when(jwtService.isRefreshTokenValid("my-refresh-token-jwt")).thenReturn(true);
        when(jwtService.getJtiFromRefreshToken("my-refresh-token-jwt")).thenReturn("refresh-jti-888");
        when(userRefreshTokenRepository.findByJti("refresh-jti-888")).thenReturn(java.util.Optional.of(tokenRecord));

        authTokenService.logout(request, response);

        org.junit.jupiter.api.Assertions.assertEquals(UserRefreshTokenStatus.REVOKED, tokenRecord.getStatus());
        String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("refreshToken="));
        assertTrue(setCookieHeader.contains("Max-Age=0"));
    }

    @Test
    @DisplayName("Should gracefully handle logout when no Authorization header is present")
    void logout_whenNoBearerToken_gracefullyHandlesAndStillClearsCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        authTokenService.logout(request, response);

        verifyNoInteractions(cacheManager);
        String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("refreshToken="));
        assertTrue(setCookieHeader.contains("Max-Age=0"));
    }

    @Test
    @DisplayName("Should revoke all refresh tokens in DB and blacklist user active access tokens in cache")
    void revokeAllForUser_revokesDbRecordsAndCachesUserRevocationTimestamp() {
        when(cacheManager.getCache(CACHE_REVOKED_ACCESS_TOKENS)).thenReturn(revokedAccessTokensCache);

        authTokenService.revokeAllForUser(42L);

        verify(userRefreshTokenRepository).revokeAllForUser(42L, UserRefreshTokenStatus.REVOKED);
        verify(revokedAccessTokensCache).put(eq("user:42"), any(Long.class));
    }
}
