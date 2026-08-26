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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static com.example.ecom.common.utils.Utils.generatePassword;
import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    public static final String GOOGLE_USER_INFO_API = "https://www.googleapis.com/oauth2/v3/userinfo?access_token={access_token}";

    public static final String ACCESS_TOKEN_KEY = "access_token";

    private final WebClient webClient;

    private final AuthTokenService authTokenService;

    private final AuthRepository authRepository;

    private final RoleService roleService;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TokenDto loginWithGoogle(GoogleLoginRequest request) {
        String token = request.token();

        if (!hasText(token)) {
            throw new BadCredentialsException("Invalid google login token");
        }

        Map<String, String> params = new HashMap<>();
        params.put(ACCESS_TOKEN_KEY, token);

        GoogleUserDto googleUser = webClient.get()
                .uri(GOOGLE_USER_INFO_API, params)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new BadCredentialsException("Failed to validate Google token")
                                ))
                )
                .bodyToMono(GoogleUserDto.class)
                .block();

        if (googleUser == null || !hasText(googleUser.getEmail()) || !googleUser.isEmailVerified()) {
            throw new BadCredentialsException("Google account email not verified");
        }

        User user = authRepository.findByEmail(googleUser.getEmail()).orElse(null);

        if (isNull(user)) {
            user = new User();
            String name = hasText(googleUser.getName()) ? googleUser.getName() : googleUser.getEmail();
            user.setName(name);
            user.setEmail(googleUser.getEmail());
            user.setStatus(UserStatus.ACTIVE);
            user.setPassword(passwordEncoder.encode(generatePassword(name)));

            Role role = roleService.findByName(RoleName.USER.getValue());
            user.getRoles().add(role);

            user = authRepository.save(user);
        }

        return authTokenService.getAuthTokens(user);
    }
}
