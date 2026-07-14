package com.example.gateway.filter;

import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter implements WebFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String BROWSER_NAME_HEADER = "User-Agent";

    public static final String MDC_REQUEST_ID_KEY = "requestId";
    public static final String MDC_USER_ID_KEY = "userId";
    public static final String MDC_CLIENT_IP_KEY = "clientIp";

    @PostConstruct
    public void init() {
        Hooks.enableAutomaticContextPropagation();
        
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                MDC_REQUEST_ID_KEY,
                () -> MDC.get(MDC_REQUEST_ID_KEY),
                val -> MDC.put(MDC_REQUEST_ID_KEY, val),
                () -> MDC.remove(MDC_REQUEST_ID_KEY)
        );
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                MDC_USER_ID_KEY,
                () -> MDC.get(MDC_USER_ID_KEY),
                val -> MDC.put(MDC_USER_ID_KEY, val),
                () -> MDC.remove(MDC_USER_ID_KEY)
        );
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                MDC_CLIENT_IP_KEY,
                () -> MDC.get(MDC_CLIENT_IP_KEY),
                val -> MDC.put(MDC_CLIENT_IP_KEY, val),
                () -> MDC.remove(MDC_CLIENT_IP_KEY)
        );
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String originalRequestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        final String requestId = (originalRequestId == null || originalRequestId.isEmpty())
                ? UUID.randomUUID().toString()
                : originalRequestId;

        final String ip = Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress();

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(REQUEST_ID_HEADER, requestId)
                .build();

        String userAgent = request.getHeaders().getFirst(BROWSER_NAME_HEADER);
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String query = request.getURI().getQuery();
        String fullPath = query != null ? path + "?" + query : path;

        long startTime = System.nanoTime();

        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        return chain.filter(exchange.mutate().request(request).build())
                .doFirst(() -> {
                    populateMdc(requestId, ip);
                    log.info("Request Started: {} {} | UA: {}", method, fullPath, userAgent);
                })
                .doFinally(signalType -> {
                    try {
                        long duration = Duration.ofNanos(System.nanoTime() - startTime).toMillis();
                        populateMdc(requestId, ip);

                        Object userId = exchange.getAttribute(MDC_USER_ID_KEY);
                        if (userId != null) {
                            MDC.put(MDC_USER_ID_KEY, userId.toString());
                        }

                        int status = exchange.getResponse().getStatusCode() != null
                                ? exchange.getResponse().getStatusCode().value()
                                : 200;

                        log.info("Request Ended: {} {} with status {} in {}ms", method, fullPath, status, duration);
                    } finally {
                        MDC.clear();
                    }
                })
                .contextWrite(Context.of(
                        MDC_REQUEST_ID_KEY, requestId,
                        MDC_CLIENT_IP_KEY, ip
                ));
    }

    private void populateMdc(String requestId, String clientIp) {
        MDC.put(MDC_REQUEST_ID_KEY, requestId);
        if (clientIp != null) {
            MDC.put(MDC_CLIENT_IP_KEY, clientIp);
        }
    }
}
