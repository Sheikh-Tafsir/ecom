package com.example.ecom.common.config;

import com.example.ecom.common.filter.AuthenticationFilter;
import com.example.ecom.common.filter.UserTrafficControlFilter;
import com.example.ecom.common.service.CustomUserDetailsService;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
            "/actuator",
            "/actuator/**"
    );

    @Value("${app.standalone:true}")
    private boolean isStandAloneServer;

    @Value("${cors.allowed.origins}")
    private String allowedOrigins;

    private final CustomUserDetailsService userDetailsService;

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

                        .requestMatchers("/auth/**").permitAll()

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
}
