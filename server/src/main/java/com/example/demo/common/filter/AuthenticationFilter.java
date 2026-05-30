package com.example.demo.common.filter;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.service.CustomUserDetailsService;
import com.example.demo.common.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.example.demo.common.config.SecurityConfig.AUTH_URL;
import static com.example.demo.common.config.SecurityConfig.PUBLIC_URLS;
import static com.example.demo.common.utils.ResponseUtils.error;

@Slf4j
@RequiredArgsConstructor
@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    private final CustomUserDetailsService userDetailsService;

    private final AntPathMatcher pathMatcher;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return pathMatcher.match(AUTH_URL, request.getServletPath())
                || PUBLIC_URLS.stream().anyMatch(pattern -> pathMatcher.match(pattern, request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String path = request.getServletPath();
        log.debug("Processing authentication for path: {}", path);

        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            log.debug("No Bearer token found in request headers");
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        try {
            if (!jwtService.isAccessTokenValid(token)) {
                error(response, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String email = jwtService.getEmailFromAccessToken(token);
                CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            chain.doFilter(request, response);
        } catch (Exception ex) {
            error(response, HttpStatus.UNAUTHORIZED, "Error validating JWT token");
        }
    }
}
