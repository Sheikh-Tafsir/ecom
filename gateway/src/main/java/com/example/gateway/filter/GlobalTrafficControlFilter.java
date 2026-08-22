package com.example.gateway.filter;

import com.example.gateway.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class GlobalTrafficControlFilter implements WebFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (RateLimiterService.shouldBypassRateLimit(exchange)) {
            return chain.filter(exchange);
        }

        // 1. Check Global Rate Limit
        return rateLimiterService.isGlobalRateAllowed()
                .flatMap(rateAllowed -> {
                    if (!rateAllowed) {
                        log.error("Too many global requests");
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }

                    // 2. Concurrency Throttle (Bulkhead)
                    return rateLimiterService.acquireGlobalConcurrency()
                            .flatMap(concurrencyAllowed -> {
                                if (!concurrencyAllowed) {
                                    log.error("Global concurrency limit exceeded");
                                    exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                                    return exchange.getResponse().setComplete();
                                }

                                return chain.filter(exchange)
                                        .doFinally(signalType -> rateLimiterService.releaseGlobalConcurrency().subscribe());
                            });
                });
    }
}
