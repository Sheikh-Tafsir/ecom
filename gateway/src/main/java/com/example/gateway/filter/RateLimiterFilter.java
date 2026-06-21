package com.example.gateway.filter;

import com.example.gateway.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiterFilter implements WebFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String ip = Objects.requireNonNull(request.getRemoteAddress())
                .getAddress()
                .getHostAddress();

        return getEmailFromContext()
                .defaultIfEmpty("")
                .flatMap(email ->
                        rateLimiterService.isRequestAllowed(email, ip)
                                .flatMap(allowed -> {

                                    if (!allowed) {
                                        exchange.getResponse()
                                                .setStatusCode(HttpStatus.TOO_MANY_REQUESTS);

                                        return exchange.getResponse().setComplete();
                                    }

                                    return chain.filter(exchange);
                                })
                );
    }

    private Mono<String> getEmailFromContext() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .ofType(String.class);
    }
}
