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

import static com.example.demo.common.filter.LoggingFilter.MDC_USER_ID_KEY;
import static com.example.demo.common.utils.ResponseUtils.error;

@Slf4j
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    private static final String ACCESS_TOKEN = "accessToken";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws IOException, ServletException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = null;

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length());
        } else {
            token = request.getParameter(ACCESS_TOKEN);
        }

        if (!StringUtils.hasText(token)) {
            log.debug("No auth token found in request headers or parameters");
            chain.doFilter(request, response);
            return;
        }

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

                MDC.put(MDC_USER_ID_KEY, userDetails.getId().toString());
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Invalid or expired JWT token", e);
            error(response, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
        }
    }
}
