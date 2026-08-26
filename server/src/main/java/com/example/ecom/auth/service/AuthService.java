package com.example.ecom.auth.service;

import com.example.ecom.auth.dto.*;
import com.example.ecom.auth.enums.OtpType;
import com.example.ecom.auth.repository.AuthRepository;
import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.enums.RoleName;
import com.example.ecom.common.enums.UserStatus;
import com.example.ecom.common.model.Role;
import com.example.ecom.common.model.User;
import com.example.ecom.common.service.mail.MailService;
import com.example.ecom.user.role.service.RoleService;
import com.example.ecom.user.user.service.UserService;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String FORGET_PASSWORD_MAIL_SUBJECT = "Forget password process";

    public static final String FORGET_PASSWORD_MAIL_TEXT = "Your One time password for forget password is below:\n";

    public static final String SIGNUP_MAIL_SUBJECT = "Signup process";

    public static final String SIGNUP_MAIL_TEXT = "Your One time password for password for signup is below:\n";

    private final AuthRepository authRepository;

    private final MailService mailService;

    private final OtpService otpService;

    private final RoleService roleService;

    private final UserService userService;

    private final AuthTokenService authTokenService;

    private final AuthenticationManager authenticationManager;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TokenDto login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        if (!userDetails.isEnabled()) {
            throw new BadCredentialsException("Account is not active");
        }

        User user = userService.findByIdHelper(userDetails.getId());

        return authTokenService.getAuthTokens(user);
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

        Role role = roleService.findByName(RoleName.USER.getValue());
        user.getRoles().add(role);
        user.setStatus(UserStatus.ACTIVE);
        user = authRepository.save(user);

        return authTokenService.getAuthTokens(user);
    }

    public void forgetPassword(OtpRequest request) {
        User user = findByEmail(request.email());

        if (isNull(user)) {
            throw new ValidationException("Email is invalid");
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
        authTokenService.revokeAllForUser(user.getId());
    }

    private User findByEmail(String email) {
        return authRepository.findByEmail(email).orElse(null);
    }
    
    private User save(User user, String password) {
        user.setPassword(passwordEncoder.encode(password));
        return authRepository.save(user);
    }
}
