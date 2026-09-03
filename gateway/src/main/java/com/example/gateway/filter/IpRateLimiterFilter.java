package com.example.gateway.filter;

import com.example.gateway.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class IpRateLimiterFilter implements WebFilter {

    private static final String AUTH_PATH = "/auth";

    private final RateLimiterService rateLimiterService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        if (RateLimiterService.shouldBypassRateLimit(exchange)) {
            return chain.filter(exchange);
        }

        String ip = extractIp(exchange.getRequest());

        if (ip == null || ip.isBlank()) {
            log.warn("Missing client IP");
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }

        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        boolean isAuthPath = path != null && path.startsWith(AUTH_PATH);

        // 1. IP Rate Limit
        return rateLimiterService.isIpRateAllowed(ip, isAuthPath)
                .flatMap(rateAllowed -> {
                    if (!rateAllowed) {
                        log.error("Too many requests from IP: {}", ip);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }

                    // 2. IP Concurrency Throttle
                    return rateLimiterService.acquireIpConcurrency(ip, isAuthPath)
                            .flatMap(concurrencyAllowed -> {
                                if (!concurrencyAllowed) {
                                    log.error("Too many simultaneous connections from IP: {}", ip);
                                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                    return exchange.getResponse().setComplete();
                                }

                                return Mono.usingWhen(
                                        Mono.just(true),
                                        acquired -> chain.filter(exchange),
                                        acquired -> rateLimiterService.releaseIpConcurrency(ip, isAuthPath),
                                        (acquired, err) -> rateLimiterService.releaseIpConcurrency(ip, isAuthPath),
                                        acquired -> rateLimiterService.releaseIpConcurrency(ip, isAuthPath)
                                );
                            });
                });
    }

    public static String extractIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return null;
    }
}
