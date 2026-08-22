package com.example.gateway.filter;

import com.example.gateway.dto.CustomUserDetails;
import com.example.gateway.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class UserRateLimiterFilter implements WebFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        if (RateLimiterService.shouldBypassRateLimit(exchange)) {
            return chain.filter(exchange);
        }

        return getEmailFromContext()
                .defaultIfEmpty("")
                .flatMap(email -> {
                    if (email == null || email.isBlank()) {
                        return chain.filter(exchange);
                    }

                    // 1. User Rate Limit
                    return rateLimiterService.isUserRateAllowed(email)
                            .flatMap(rateAllowed -> {
                                if (!rateAllowed) {
                                    log.error("Too many requests from user: {}", email);
                                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                    return exchange.getResponse().setComplete();
                                }

                                // 2. User Concurrency Throttle
                                return rateLimiterService.acquireUserConcurrency(email)
                                        .flatMap(concurrencyAllowed -> {
                                            if (!concurrencyAllowed) {
                                                log.error("Too many simultaneous requests for user: {}", email);
                                                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                                return exchange.getResponse().setComplete();
                                            }

                                            return chain.filter(exchange)
                                                    .doFinally(signalType -> rateLimiterService.releaseUserConcurrency(email).subscribe());
                                        });
                            });
                });
    }

    private Mono<String> getEmailFromContext() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof CustomUserDetails)
                .cast(CustomUserDetails.class)
                .map(CustomUserDetails::getEmail);
    }
}
