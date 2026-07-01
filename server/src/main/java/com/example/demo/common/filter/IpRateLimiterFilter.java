package com.example.demo.common.filter;

import com.example.demo.common.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.example.demo.common.utils.ResponseUtils.error;

@Slf4j
@RequiredArgsConstructor
public class IpRateLimiterFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String ip = request.getRemoteAddr();

        if (ip == null || ip.trim().isEmpty()) {
            log.warn("Missing client IP");
            reject(response);
            return;
        }

        if (!rateLimiterService.isIpAllowed(ip, uri)) {
            reject(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        error(response, HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Try again later");
    }
}
