package com.example.ecom.auth.service;

import com.example.ecom.auth.dto.GoogleLoginRequest;
import com.example.ecom.auth.dto.GoogleUserDto;
import com.example.ecom.auth.dto.TokenDto;
import com.example.ecom.auth.repository.AuthRepository;
import com.example.ecom.common.enums.RoleName;
import com.example.ecom.common.enums.UserStatus;
import com.example.ecom.common.model.Role;
import com.example.ecom.common.model.User;
import com.example.ecom.user.role.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

import static com.example.ecom.common.utils.Utils.generateSecureRandomPassword;
import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Service
public class OAuthService {

    public static final String GOOGLE_TOKEN_INFO_API = "https://oauth2.googleapis.com/tokeninfo";
    public static final String GOOGLE_USER_INFO_API = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final WebClient webClient;

    private final AuthTokenService authTokenService;

    private final AuthRepository authRepository;

    private final RoleService roleService;

    private final PasswordEncoder passwordEncoder;

    private final String googleClientId;

    public OAuthService(
            WebClient webClient,
            AuthTokenService authTokenService,
            AuthRepository authRepository,
            RoleService roleService,
            PasswordEncoder passwordEncoder,
            @Value("${oauth.google.client-id:${GOOGLE_LOGIN_CLIENT_ID:}}") String googleClientId
    ) {
        this.webClient = webClient;
        this.authTokenService = authTokenService;
        this.authRepository = authRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.googleClientId = googleClientId;
    }

    @Transactional
    public TokenDto loginWithGoogle(GoogleLoginRequest request) {
        String token = request != null ? request.token() : null;

        if (!hasText(token)) {
            throw new BadCredentialsException("Invalid google login token");
        }

        if (!hasText(googleClientId)) {
            log.error("Google OAuth login attempted but google.clientId is not configured on the server");
            throw new BadCredentialsException("Google OAuth is not configured on this server");
        }

        List<String> allowedClientIds = Arrays.stream(googleClientId.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        GoogleUserDto tokenInfo = fetchTokenInfo(token);

        if (tokenInfo == null) {
            throw new BadCredentialsException("Failed to validate Google token");
        }

        if (!tokenInfo.matchesAudience(allowedClientIds)) {
            log.warn("Google token audience mismatch! Allowed: {}, Token aud: {}, Token azp: {}",
                    allowedClientIds, tokenInfo.getAudience(), tokenInfo.getAzp());
            throw new BadCredentialsException("Invalid Google token: audience mismatch");
        }

        if (!hasText(tokenInfo.getEmail()) || !tokenInfo.isEmailVerified()) {
            throw new BadCredentialsException("Google account email not verified");
        }

        String name = tokenInfo.getName();

        if (!hasText(name)) {
            GoogleUserDto userInfo = fetchUserInfo(token);
            if (userInfo != null && hasText(userInfo.getName())) {
                name = userInfo.getName();
            } else {
                name = tokenInfo.getEmail();
            }
        }

        User user = authRepository.findByEmail(tokenInfo.getEmail()).orElse(null);

        if (isNull(user)) {
            user = new User();
            user.setName(name);
            user.setEmail(tokenInfo.getEmail());
            user.setStatus(UserStatus.ACTIVE);
            user.setPassword(passwordEncoder.encode(generateSecureRandomPassword()));

            Role role = roleService.findByName(RoleName.USER.getValue());
            user.getRoles().add(role);

            user = authRepository.save(user);
        }

        return authTokenService.getAuthTokens(user);
    }

    protected GoogleUserDto fetchTokenInfo(String token) {
        boolean isIdToken = isJwt(token);
        String paramKey = isIdToken ? "id_token" : "access_token";

        try {
            return webClient.get()
                    .uri(GOOGLE_TOKEN_INFO_API + "?" + paramKey + "={token}", token)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("Google token validation failed: {}", body);
                                        return Mono.error(new BadCredentialsException("Failed to validate Google token"));
                                    })
                    )
                    .bodyToMono(GoogleUserDto.class)
                    .block();
        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to connect to Google tokeninfo API: {}", e.getMessage());
            throw new BadCredentialsException("Failed to validate Google token");
        }
    }

    protected GoogleUserDto fetchUserInfo(String token) {
        try {
            return webClient.get()
                    .uri(GOOGLE_USER_INFO_API)
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> Mono.empty()
                    )
                    .bodyToMono(GoogleUserDto.class)
                    .block();
        } catch (Exception e) {
            log.warn("Could not retrieve extended userinfo from Google: {}", e.getMessage());
            return null;
        }
    }

    protected boolean isJwt(String token) {
        if (token == null) return false;
        return token.chars().filter(ch -> ch == '.').count() == 2;
    }
}
