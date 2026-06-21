package com.example.demo.auth.service;

import com.example.demo.auth.dto.*;
import com.example.demo.auth.repository.AuthRepository;
import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.auth.enums.OtpType;
import com.example.demo.common.enums.UserStatus;
import com.example.demo.common.exception.InvalidRefreshTokenException;
import com.example.demo.common.model.Role;
import com.example.demo.common.model.User;
import com.example.demo.auth.dto.Otp;
import com.example.demo.common.service.JwtService;
import com.example.demo.common.service.MailService;
import com.example.demo.role.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static com.example.demo.common.utils.CookieUtils.addCookie;
import static com.example.demo.common.utils.CookieUtils.getCookieValue;
import static com.example.demo.common.utils.SecurityConstants.*;
import static com.example.demo.common.utils.Utils.*;
import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String FORGET_PASSWORD_MAIL_SUBJECT = "Forget password process";

    public static final String FORGET_PASSWORD_MAIL_TEXT = "Your One time password for forget password is below:\n";

    public static final String SIGNUP_MAIL_SUBJECT = "Signup process";

    public static final String SIGNUP_MAIL_TEXT = "Your One time password for password for signup is below:\n";

    public static final String GOOGLE_OAUTH_API= "https://www.googleapis.com/oauth2/v1/userinfo?access_token={access_token}";

    @Value("${refresh.token.name}")
    private String refreshTokenName;

    @Value("${refresh.token.validity}")
    private long refreshTokenValidity;

    private final AuthRepository authRepository;

    private final MailService mailService;

    private final JwtService jwtService;

    private final OtpService otpService;

    private final RoleService roleService;

    private final AuthenticationManager authenticationManager;

    private final WebClient webClient;

    private final PasswordEncoder passwordEncoder;

    public TokenDto login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        if (userDetails.user().isNotActive()) {
            throw new BadCredentialsException("Account is " + userDetails.user().getStatus().getValue());
        }

        return getTokens(userDetails.user());
    }

    @Transactional
    public void signup(SignupRequest signupRequest) {
        User user = findByEmail(signupRequest.email());

        if (user != null) {
            throw new BadCredentialsException("Email already exists! Account is " + user.getStatus().getValue());
        }

        user = new User();
        user.setName(signupRequest.name());
        user.setEmail(signupRequest.email());
        user = save(user, signupRequest.password());
        
        Otp otp = otpService.createOtp(user, OtpType.SIGNUP);
        mailService.sendEmailAsync(user.getEmail(), SIGNUP_MAIL_SUBJECT, SIGNUP_MAIL_TEXT + otp.getValue());
    }

    public void resendSignupOtp(OtpRequest request) {
        User user = findByEmail(request.email());

        if (isNull(user)) {
            throw new BadCredentialsException("Email is invalid");
        }

        if (user.getStatus() != UserStatus.NOT_VERIFIED) {
            throw new BadCredentialsException("Email already exists! Account is " + user.getStatus().getValue());
        }

        Otp otp = otpService.getOrCreateOtp(user, OtpType.SIGNUP);
        mailService.sendEmailAsync(user.getEmail(), SIGNUP_MAIL_SUBJECT, SIGNUP_MAIL_TEXT + otp.getValue());
    }

    @Transactional
    public TokenDto verifySignupOtp(VerifySignupOtpRequest request) {
        User user = findByEmail(request.email());
        if (isNull(user)) {
            throw new BadCredentialsException("User not found");
        }

        if (user.getStatus() != UserStatus.NOT_VERIFIED) {
            throw new BadCredentialsException("Email already exists! Account is " + user.getStatus().getValue());
        }

        otpService.verifyOtp(OtpType.SIGNUP, request.otp(), request.email());

        Role role = roleService.findByName(ROLE_USER);
        user.getRoles().add(role);
        user.setStatus(UserStatus.ACTIVE);
        user = authRepository.save(user);

        return getTokens(user);
    }

    @Transactional
    public TokenDto loginWithGoogle(Map<String, String> request) {
        String token = request.get("token");

        if (!hasText(token)) {
            throw new BadCredentialsException("Invalid google login token");
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", token);

        GoogleUserDto googleUser = webClient.get()
                .uri(GOOGLE_OAUTH_API, params)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new BadCredentialsException("Failed to fetch Google user info")
                                ))
                )
                .bodyToMono(GoogleUserDto.class)
                .block();

        if (googleUser == null || !googleUser.isVerified_email()) {
            throw new BadCredentialsException("Google account email not verified");
        }

        User user = findByEmail(googleUser.getEmail());

        if (isNull(user)) {
            user = new User();
            user.setName(googleUser.getName());
            user.setEmail(googleUser.getEmail());
            user.setStatus(UserStatus.ACTIVE);

            Role role = roleService.findByName(ROLE_USER);
            user.getRoles().add(role);

            user = save(user, generatePassword(googleUser.getName()));
        }

        return getTokens(user);
    }

    public void addRefreshCookie(HttpServletResponse response, TokenDto authResponse) {
        addCookie(response, refreshTokenName, authResponse.getRefreshToken(), refreshTokenValidity/1000);
    }

    public String refreshAccessToken(HttpServletRequest request) {
        String refreshToken = getCookieValue(request, refreshTokenName);

        if (!hasText(refreshToken)) {
            throw new InvalidRefreshTokenException("Refresh token cookie not found");
        }

        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        String cookieEmail = jwtService.getEmailFromRefreshToken(refreshToken);
        User user = findByEmail(cookieEmail);

        if (isNull(user) || user.isNotActive()) {
            throw new InvalidRefreshTokenException("Refresh token user is invalid");
        }

        return jwtService.generateAccessToken(user);
    }

    public void logout(HttpServletResponse response) {
        addCookie(response, refreshTokenName, null, 0);
    }

    public void forgetPassword(OtpRequest request) {
        User user = findByEmail(request.email());

        if (isNull(user)) {
            throw new BadCredentialsException("Email is invalid");
        }

        Otp userOtp = otpService.getOrCreateOtp(user, OtpType.FORGET);
        mailService.sendEmailAsync(user.getEmail(), FORGET_PASSWORD_MAIL_SUBJECT, FORGET_PASSWORD_MAIL_TEXT + userOtp.getValue());
    }

    @Transactional
    public void verifyForgetPasswordOtp(VerifyForgetPasswordOtpRequest request) {
        User user = findByEmail(request.email());

        if (isNull(user)) {
            throw new BadCredentialsException("User not found");
        }

        otpService.verifyOtp(OtpType.FORGET, request.otp(), request.email());

        save(user, request.password());
    }

    public TokenDto getTokens(User user) {
        if (user.isNotActive()) {
            throw new ValidationException("User is " + user.getStatus().getValue());
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return TokenDto.builder().accessToken(accessToken).refreshToken(refreshToken).build();
    }

    private User findByEmail(String email) {
        return authRepository.findByEmail(email).orElse(null);
    }
    
    private User save(User user, String password) {
        user.setPassword(passwordEncoder.encode(password));
        return authRepository.save(user);
    }

    private void checkActive(User user) {
        if (user.getDeleted()) {
            throw new RuntimeException("User is deleted");
        }
    }
}
