package com.example.gateway.filter;

import com.example.gateway.dto.CustomUserDetails;
import com.example.gateway.service.JwtService;
import com.example.gateway.util.ResponseUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.example.gateway.filter.LoggingFilter.MDC_USER_ID_KEY;

@Slf4j
@RequiredArgsConstructor
public class AuthenticationFilter implements WebFilter {

    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    private final String revokedTokensPrefix;

    private static final String ACCESS_TOKEN = "accessToken";

    // Local cache to reduce Redis traffic:
    // Blacklisted JTIs (true) are cached for 10 minutes.
    // Non-blacklisted JTIs (false) are cached for 15 seconds.
    // SECURITY NOTE: The 15-second cache for non-blacklisted tokens means a freshly revoked token
    // can still pass through for up to 15 seconds until the cache entry expires.
    // This is an accepted trade-off for performance (reduces Redis round-trips).
    private final Cache<String, Boolean> blacklistStatusCache = Caffeine.newBuilder()
            .expireAfter(new Expiry<String, Boolean>() {
                @Override
                public long expireAfterCreate(String key, Boolean value, long currentTime) {
                    if (Boolean.TRUE.equals(value)) {
                        return TimeUnit.MINUTES.toNanos(10);
                    } else {
                        return TimeUnit.SECONDS.toNanos(15);
                    }
                }

                @Override
                public long expireAfterUpdate(String key, Boolean value, long currentTime, long currentDuration) {
                    return currentDuration;
                }

                @Override
                public long expireAfterRead(String key, Boolean value, long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .maximumSize(20000)
            .build();

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = null;

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length());
        } else {
            token = exchange.getRequest().getQueryParams().getFirst(ACCESS_TOKEN);
        }

        if (!StringUtils.hasText(token)) {
            log.debug("No auth token found in request headers or parameters");
            return chain.filter(exchange);
        }

        try {
            Claims claims = jwtService.parseAccessTokenClaims(token);
            String jti = claims.getId();

            // Enforce JTI claim presence for defense-in-depth security
            if (!StringUtils.hasText(jti)) {
                log.warn("Access token lacks a mandatory JTI (JWT ID) claim");
                return ResponseUtils.error(exchange, HttpStatus.UNAUTHORIZED, "Access token lacks mandatory JTI claim");
            }

            // 1. Check local cache first to avoid Redis traffic
            Boolean cachedStatus = blacklistStatusCache.getIfPresent(jti);
            if (cachedStatus != null) {
                if (Boolean.TRUE.equals(cachedStatus)) {
                    log.warn("Access token JTI: {} is revoked/blacklisted (local cache hit)", jti);
                    return ResponseUtils.error(exchange, HttpStatus.UNAUTHORIZED, "Access token has been revoked");
                }
                return proceedWithAuthentication(exchange, chain, claims);
            }

            // 2. Query Redis if not in local cache
            String cacheKey = revokedTokensPrefix + jti;
            return reactiveStringRedisTemplate.hasKey(cacheKey)
                    .map(Optional::of)
                    .onErrorResume(e -> {
                        // Fail closed: deny access when we cannot verify token revocation status.
                        // Allowing a potentially revoked token through is a security risk.
                        log.error("Redis error checking revoked tokens blacklist for JTI {}: {}", jti, e.getMessage());
                        return Mono.just(Optional.<Boolean>empty());
                    })
                    .flatMap(optBlacklisted -> {
                        if (optBlacklisted.isEmpty()) {
                            return ResponseUtils.error(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                                    "Service temporarily unavailable — unable to verify token status");
                        }

                        boolean isBlacklisted = optBlacklisted.get();
                        // Populate local cache only on successful Redis check
                        blacklistStatusCache.put(jti, isBlacklisted);

                        if (isBlacklisted) {
                            log.warn("Access token JTI: {} is revoked/blacklisted", jti);
                            return ResponseUtils.error(exchange, HttpStatus.UNAUTHORIZED, "Access token has been revoked");
                        }
                        return proceedWithAuthentication(exchange, chain, claims);
                    });
        } catch (Exception e) {
            log.error("Invalid or expired JWT token: {}", e.getMessage());
            return ResponseUtils.error(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired access token");
        }
    }

    private Mono<Void> proceedWithAuthentication(ServerWebExchange exchange, WebFilterChain chain, Claims claims) {
        CustomUserDetails userDetails = new CustomUserDetails(claims);

        if (!userDetails.isEnabled()) {
            log.warn("User is not active: {}", userDetails.getEmail());
            return ResponseUtils.error(exchange, HttpStatus.UNAUTHORIZED, "User account is inactive");
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        exchange.getAttributes().put(MDC_USER_ID_KEY, userDetails.getId());

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx
                        .put(MDC_USER_ID_KEY, userDetails.getId().toString())
                        .putAll(ReactiveSecurityContextHolder.withAuthentication(authentication))
                );
    }
}
