package com.example.demo.auth.service;

import com.example.demo.auth.dto.*;
import com.example.demo.auth.repository.OtpRepository;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.enums.OtpType;
import com.example.demo.common.enums.UserStatus;
import com.example.demo.common.exception.InvalidRefreshTokenException;
import com.example.demo.common.model.User;
import com.example.demo.common.model.Otp;
import com.example.demo.common.service.JwtService;
import com.example.demo.common.service.MailService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.example.demo.common.utils.CookieUtils.addCookie;
import static com.example.demo.common.utils.Utils.*;
import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String RESET_PASSWORD_MAIL_SUBJECT = "Reset password process";

    public static final String RESET_PASSWORD_MAIL_TEXT = "Your One time password for password reset is below:\n";

    public static final String SIGNUP_MAIL_SUBJECT = "Signup process";

    public static final String SIGNUP_MAIL_TEXT = "Your One time password for password for signup is below:\n";

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    public static final int OTP_TRY_AGAIN_WAITING_TIME = 30;

    public static final String GOOGLE_OAUTH_API= "https://www.googleapis.com/oauth2/v1/userinfo?access_token={access_token}";

    @Value("${refresh.cookie.validity}")
    private long refreshCookieValidity;

    private final UserRepository userRepository;

    private final OtpRepository otpRepository;

    private final MailService mailService;

    private final JwtService jwtService;

    private final AuthenticationManager authenticationManager;

    private final RestTemplate restTemplate;

    private final ModelMapper modelMapper;

    private final PasswordEncoder passwordEncoder;

    public TokenDto login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return getTokens(userDetails.user());
    }

    @Transactional
    public void signup(SignupRequest signupRequest) {
        User user = findByEmail(signupRequest.email());

        if (user != null) {
            if (user.getStatus() == UserStatus.ACTIVE) {
                throw new BadCredentialsException("Account already exists!");
            } else {
                throw new BadCredentialsException("Account already exists but is " + user.getStatus().getValue());
            }
        }

        user = modelMapper.map(signupRequest, User.class);
        user = save(user, signupRequest.password());
        
        Otp otp = saveOtp(user);
        mailService.sendEmail(user.getEmail(), SIGNUP_MAIL_SUBJECT, SIGNUP_MAIL_TEXT + otp.getValue());
    }

    @Transactional
    public TokenDto verifySignupOtp(VerifySignupOtpRequest verifySignupOtpRequest) {
        Otp otp = verifyOtp(OtpType.SIGNUP, verifySignupOtpRequest.otp(), verifySignupOtpRequest.email());

        User user = otp.getUser();
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

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

        GoogleUserDto googleUser;
        try {
            ResponseEntity<GoogleUserDto> response = restTemplate.getForEntity(
                    GOOGLE_OAUTH_API,
                    GoogleUserDto.class,
                    params
            );

            googleUser = response.getBody();
        } catch (HttpClientErrorException e) {
            throw new BadCredentialsException("Failed to fetch Google user info");
        }

        if (isNull(googleUser) || !googleUser.isVerified_email()) {
            throw new BadCredentialsException("Google account email not verified");
        }

        User existingUser = findByEmail(googleUser.getEmail());

        if (isNull(existingUser)) {
            User user = new User();
            user.setName(googleUser.getName());
            user.setEmail(googleUser.getEmail());
            user.setStatus(UserStatus.ACTIVE);

            existingUser = save(user, generatePassword(googleUser.getName()));
        }

        return getTokens(existingUser);
    }

    public void addRefreshCookie(HttpServletResponse response, TokenDto authResponse) {
        addCookie(response, REFRESH_TOKEN_COOKIE_NAME, authResponse.getRefreshToken(), refreshCookieValidity);
    }

    public String refreshAccessToken(String refreshToken) {
        if (!hasText(refreshToken)) {
            throw new InvalidRefreshTokenException("Refresh token cookie not found");
        }

        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        String cookieEmail = jwtService.getEmailFromRefreshToken(refreshToken);
        User user = findByEmail(cookieEmail);

        if (isNull(user) || !user.isActive()) {
            throw new InvalidRefreshTokenException("Refresh token user is invalid");
        }

        return jwtService.generateAccessToken(user);
    }

    public void logout(HttpServletResponse response) {
        addCookie(response, REFRESH_TOKEN_COOKIE_NAME, null, 0);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {
        User user = findByEmail(resetPasswordRequest.email());

        if (isNull(user)) {
            throw new BadCredentialsException("Email is invalid");
        }

        Otp userOtp = resetOtp(user);

        mailService.sendEmail(user.getEmail(), RESET_PASSWORD_MAIL_SUBJECT, RESET_PASSWORD_MAIL_TEXT + userOtp.getValue());
    }

    @Transactional
    public void verifyResetPasswordOtp(VerifyResetPasswordOtpRequest resetPasswordOtpRequest) {
        Otp otp = verifyOtp(OtpType.RESET, resetPasswordOtpRequest.otp(), resetPasswordOtpRequest.email());

        User user = otp.getUser();
        save(user, resetPasswordOtpRequest.password());
    }

    public TokenDto getTokens(User user) {
        if (!user.isActive()) {
            throw new ValidationException("User is " + user.getStatus().getValue());
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return TokenDto.builder().accessToken(accessToken).refreshToken(refreshToken).build();
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    
    private User save(User user, String password) {
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }
    
    private Otp saveOtp(User user) {
        Otp otp = new Otp();

        otp.setUser(user);
        otp.setType(OtpType.SIGNUP);
        otp.setValue(generateOtp());

        return otpRepository.save(otp);
    }

    private Otp verifyOtp(OtpType type, int otp, String email) {
        Otp userOtp = otpRepository.findByTypeAndUser_Email(type, email);
        if (userOtp == null) {
            throw new BadCredentialsException("Invalid Email or otp type");
        }

        if (userOtp.getValue() != otp) {
            throw new BadCredentialsException("Invalid Email or otp type");
        }

        return userOtp;
    }

    private Otp resetOtp(User user) {
        Otp otp = otpRepository.findByUserAndType(user, OtpType.RESET)
                .orElseThrow(()-> new RuntimeException("Otp not found"));

        if (otp.getId() != null) {
            LocalDateTime now = LocalDateTime.now();

            if (!otp.getExpiresAt().isBefore(now)) {
                throw new BadCredentialsException("Previous OTP has not been expired");
            }

            Duration timeSinceExpiry = Duration.between(otp.getExpiresAt(), now);
            if (timeSinceExpiry.toMinutes() < OTP_TRY_AGAIN_WAITING_TIME) {
                throw new BadCredentialsException("Try again after " + (OTP_TRY_AGAIN_WAITING_TIME - timeSinceExpiry.toMinutes()) + " minutes");
            }
        }

        otp.setValue(generateOtp());

        return otpRepository.save(otp);
    }

    private int generateOtp() {
        SecureRandom secureRandom = new SecureRandom();
        return 100000 + secureRandom.nextInt(900000);
    }
}
