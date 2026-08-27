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

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = getAccessToken(request, authHeader);

        if (!StringUtils.hasText(token)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Claims claims = jwtService.parseAccessTokenClaims(token);

                String jti = claims.getId();
                if (StringUtils.hasText(jti) && cacheManager != null) {
                    Cache revokedTokensCache = cacheManager.getCache(CACHE_REVOKED_ACCESS_TOKENS);
                    if (revokedTokensCache != null && revokedTokensCache.get(jti) != null) {
                        log.warn("Access token JTI: {} is revoked/blacklisted", jti);
                        SecurityContextHolder.clearContext();
                        error(response, HttpStatus.UNAUTHORIZED, "Access token has been revoked");
                        return;
                    }
                }

                CustomUserDetails userDetails = new CustomUserDetails(claims);

                if (!userDetails.isEnabled()) {
                    log.warn("User is not active: {}", userDetails.getEmail());
                    SecurityContextHolder.clearContext();
                    error(response, HttpStatus.UNAUTHORIZED, "User is not active");
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);

                MDC.put(MDC_USER_ID_KEY, userDetails.getId().toString());
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            log.warn("Invalid or expired JWT token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            error(response, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
        }
    }

    private String getAccessToken(HttpServletRequest request, String authHeader) {
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
