package com.example.demo.common.filter;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.example.demo.common.utils.ResponseUtils.error;

@Slf4j
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    public static final String BEARER_PREFIX = "Bearer ";

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws IOException, ServletException {

        MDC.put("requestId", request.getHeader(REQUEST_ID_HEADER));
        MDC.put("method", request.getMethod());
        MDC.put("path", request.getRequestURI());

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("No auth token found in request headers");

            chain.doFilter(request, response);
            MDC.clear();
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Claims claims = jwtService.parseAccessTokenClaims(token);
                CustomUserDetails userDetails = new CustomUserDetails(claims);

                if (!userDetails.isEnabled()) {
                    log.error("User is not active: {}", userDetails.getEmail());

                    error(response, HttpStatus.UNAUTHORIZED, "User is not active");
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);

                MDC.put("userId", userDetails.getId().toString());
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Invalid or expired JWT token", e);

            error(response, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
        } finally {
            MDC.clear();
        }
    }
}
