package com.example.gateway.filter;

import com.example.gateway.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class UserRateLimiterFilter implements WebFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        return getEmailFromContext()
                .defaultIfEmpty("")
                .flatMap(email -> {
                    if (email == null || email.trim().isEmpty()) {
                        return chain.filter(exchange);
                    }

                    return rateLimiterService.isEmailAllowed(email)
                            .flatMap(allowed -> {
                                if (!allowed) {
                                    return reject(exchange);
                                }

                                return chain.filter(exchange);
                            });
                });
    }

    private Mono<String> getEmailFromContext() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .ofType(String.class);
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }
}
