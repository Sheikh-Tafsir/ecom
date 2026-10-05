package com.example.ecom.common.filter;

import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.service.JwtService;
import com.example.ecom.notification.service.NotificationService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;

import static com.example.ecom.common.filter.LoggingFilter.MDC_USER_ID_KEY;
import static com.example.ecom.common.utils.CacheConstants.CACHE_REVOKED_ACCESS_TOKENS;
import static com.example.ecom.common.utils.ResponseUtils.error;

@Slf4j
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    public static final String BEARER_PREFIX = "Bearer ";

    private static final String SSE_TICKET = "ticket";

    private final JwtService jwtService;

    private final CacheManager cacheManager;

    private final NotificationService notificationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws IOException, ServletException {

        String token = getAccessToken(request);

        if (!StringUtils.hasText(token)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                chain.doFilter(request, response);
                return;
            }

            Claims claims = jwtService.parseAccessTokenClaims(token);

            if (isJtiRevoked(claims, request, response, chain)) return;
            if (isUserSessionRevoked(claims, request, response, chain)) return;

            CustomUserDetails userDetails = new CustomUserDetails(claims);

            if (!userDetails.isEnabled()) {
                log.warn("User is not active: {}", userDetails.getEmail());
                SecurityContextHolder.clearContext();
                error(response, HttpStatus.UNAUTHORIZED, "User is not active");
                return;
            }

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
            );
            MDC.put(MDC_USER_ID_KEY, userDetails.getId().toString());

            chain.doFilter(request, response);

        } catch (Exception e) {
            log.warn("Invalid or expired JWT token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            passOrReject(request, response, chain, "Invalid or expired JWT token");
        }
    }

    /**
     * Checks whether the token's JTI (JWT ID) has been individually blacklisted
     * (e.g. after an explicit logout).
     *
     * @return true if the request was handled (revoked or passed through for logout), false to continue
     */
    private boolean isJtiRevoked(Claims claims,
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain chain) throws IOException, ServletException {
        Cache cache = getRevocationCache();
        if (cache == null) return false;

        String jti = claims.getId();
        if (!StringUtils.hasText(jti) || cache.get(jti) == null) return false;

        log.warn("Access token JTI: {} is revoked/blacklisted", jti);
        SecurityContextHolder.clearContext();
        passOrReject(request, response, chain, "Access token has been revoked");
        return true;
    }

    /**
     * Checks whether the token was issued before a global user-level revocation
     * timestamp (set on ban, role change, or password reset).
     *
     * @return true if the request was handled (revoked or passed through for logout), false to continue
     */
    private boolean isUserSessionRevoked(Claims claims,
                                          HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain chain) throws IOException, ServletException {
        Cache cache = getRevocationCache();
        if (cache == null) return false;

        String userId = claims.getSubject();
        if (!StringUtils.hasText(userId)) return false;

        Cache.ValueWrapper wrapper = cache.get("user:" + userId);
        if (wrapper == null) return false;

        Long revokedAtMilli = parseRevocationTimestamp(wrapper.get());
        if (revokedAtMilli == null) return false;

        Date issuedAt = claims.getIssuedAt();
        if (issuedAt != null && issuedAt.getTime() > revokedAtMilli) return false;

        log.warn("Access token for user {} (issuedAt={}) predates revocation timestamp {}",
                userId, issuedAt, revokedAtMilli);
        SecurityContextHolder.clearContext();
        passOrReject(request, response, chain, "Session has been revoked");
        return true;
    }

    /**
     * For logout requests, allows the filter chain to continue even when the token is revoked
     * so the server can still revoke the refresh token. For all other requests, writes a 401.
     */
    private void passOrReject(HttpServletRequest request,
                               HttpServletResponse response,
                               FilterChain chain,
                               String message) throws IOException, ServletException {
        if (isLogoutRequest(request)) {
            chain.doFilter(request, response);
        } else {
            error(response, HttpStatus.UNAUTHORIZED, message);
        }
    }

    private Cache getRevocationCache() {
        if (cacheManager == null) return null;
        return cacheManager.getCache(CACHE_REVOKED_ACCESS_TOKENS);
    }

    private boolean isLogoutRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.contains("/logout");
    }

    private Long parseRevocationTimestamp(Object val) {
        if (val instanceof Number num) return num.longValue();
        if (val instanceof String str) {
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String getAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        String ticket = request.getParameter(SSE_TICKET);
        if (StringUtils.hasText(ticket) && notificationService != null) {
            return notificationService.getAndDeleteTokenByTicket(ticket);
        }

        return null;
    }
}
