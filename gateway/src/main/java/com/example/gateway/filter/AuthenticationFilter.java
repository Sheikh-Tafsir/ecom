package com.example.gateway.filter;

import com.example.gateway.dto.CustomUserDetails;
import com.example.gateway.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static com.example.gateway.filter.LoggingFilter.MDC_USER_ID_KEY;

@Slf4j
@RequiredArgsConstructor
public class AuthenticationFilter implements WebFilter {

    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    private static final String ACCESS_TOKEN = "accessToken";

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = null;

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length());
        } else {
            token = exchange.getRequest().getQueryParams().getFirst(ACCESS_TOKEN);
        }

        if (!StringUtils.hasText(token)) {
            log.debug("No auth token found in request headers or parameters");

            return chain.filter(exchange);
        }

        try {
            Claims claims = jwtService.parseAccessTokenClaims(token);
            CustomUserDetails userDetails = new CustomUserDetails(claims);

            if (!userDetails.isEnabled()) {
                log.error("User is not active: {}", userDetails.getEmail());

                return chain.filter(exchange);
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            exchange.getAttributes().put(MDC_USER_ID_KEY, userDetails.getId());

            return chain.filter(exchange)
                    .contextWrite(ctx -> ctx.put(MDC_USER_ID_KEY, userDetails.getId().toString()).putAll(ReactiveSecurityContextHolder.withAuthentication(authentication)));
        } catch (Exception e) {
            log.error("Invalid or expired JWT token", e);

            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
