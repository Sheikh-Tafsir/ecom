package com.example.ecom.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import static com.example.ecom.common.utils.RequestUtil.getClientIp;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    public static final String MDC_REQUEST_ID_KEY = "requestId";
    public static final String MDC_USER_ID_KEY = "userId";
    public static final String MDC_CLIENT_IP_KEY = "clientIp";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        String ip = getClientIp(request);
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String redactedQuery = redactSensitiveInfo(query);
        String fullPath = redactedQuery != null ? path + "?" + redactedQuery : path;

        long startTime = System.nanoTime();

        try {
            MDC.put(MDC_REQUEST_ID_KEY, requestId);
            MDC.put(MDC_CLIENT_IP_KEY, ip);

            response.setHeader(REQUEST_ID_HEADER, requestId);

            log.info("Request Started: {} {} | UA: {}", method, fullPath, userAgent);

            filterChain.doFilter(request, response);
        } finally {
            long duration = Duration.ofNanos(System.nanoTime() - startTime).toMillis();
            int status = response.getStatus();

            log.info("Request Ended: {} {} with status {} in {}ms", method, fullPath, status, duration);
            MDC.clear();
        }
    }

    private String redactSensitiveInfo(String query) {
        if (query == null) return null;
        return query.replaceAll("(?i)(accessToken|ticket|token|password|secret|phone|currentPassword|newPassword)=[^&]+", "$1=********");
    }
}
