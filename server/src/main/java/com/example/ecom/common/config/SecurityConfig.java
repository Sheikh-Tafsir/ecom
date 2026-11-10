package com.example.ecom.common.config;

import com.example.ecom.common.filter.AuthenticationFilter;
import com.example.ecom.common.filter.UserTrafficControlFilter;
import com.example.ecom.common.service.JwtService;
import com.example.ecom.common.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.ecom.common.utils.RequestUtil;
import com.example.ecom.common.utils.SecurityUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    public static final Set<String> PUBLIC_URLS = Set.of(
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**",
            "/actuator/health"
    );

    @Value("${app.standalone:false}")
    private boolean isStandAloneServer;

    @Value("${cors.allowed.origins}")
    private String allowedOrigins;

    @Value("${app.internal.service-key}")
    private String internalServiceKey;

    @PostConstruct
    public void validateInternalServiceKey() {
        if (internalServiceKey == null || internalServiceKey.isBlank() || internalServiceKey.length() < 32) {
            throw new IllegalStateException(
                    "app.internal.service-key must be set and contain at least 32 characters");
        }
    }

    private final UserDetailsService userDetailsService;

    private final JwtService jwtService;

    @Bean
    public AuthenticationFilter authenticationFilter(org.springframework.cache.CacheManager cacheManager) {
        return new AuthenticationFilter(jwtService, cacheManager);
    }

    @Bean
    @ConditionalOnProperty(name = "app.standalone", havingValue = "true")
    public UserTrafficControlFilter userTrafficControlFilter(RateLimiterService rateLimiterService) {
        return new UserTrafficControlFilter(rateLimiterService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   Optional<UserTrafficControlFilter> userTrafficControlFilter,
                                                   AuthenticationFilter authenticationFilter) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers(PUBLIC_URLS.toArray(new String[0])).permitAll()

                        .requestMatchers("/actuator/refresh")
                            .access((authentication, context)
                                    -> getAuthorizationDecision(context))

                        .requestMatchers("/auth/**").permitAll()

                        .requestMatchers("/internal/**")
                            .access((authentication, context)
                                    -> getAuthorizationDecision(context))

                        .requestMatchers(HttpMethod.GET, "/products", "/products/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories", "/categories/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/banners", "/banners/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/blogs", "/blogs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/faqs", "/faqs/**").permitAll()
                        .requestMatchers("/payment/callback").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterAfter(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

        userTrafficControlFilter.ifPresent(filter ->
                http.addFilterAfter(filter, AuthenticationFilter.class));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    @ConditionalOnProperty(
            name = "app.standalone",
            havingValue = "true"
    )
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList()
        );
        config.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "Idempotency-Key",
                "X-XSRF-TOKEN"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private AuthorizationDecision getAuthorizationDecision(RequestAuthorizationContext context) {
        var request = context.getRequest();
        String key = request.getHeader(SecurityUtil.INTERNAL_SERVICE_KEY_HEADER);
        boolean allowed = key != null && !key.isBlank()
                && MessageDigest.isEqual(
                        key.getBytes(StandardCharsets.UTF_8),
                        internalServiceKey.getBytes(StandardCharsets.UTF_8));
        if (!allowed) {
            log.warn("Unauthorized internal access: URI={}, IP={}, HeaderPresent={}",
                    request.getRequestURI(), RequestUtil.getClientIp(request), key != null);
        }
        return new AuthorizationDecision(allowed);
    }
}
