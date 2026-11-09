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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final UUID USER_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String USER_EMAIL = "john.doe@example.com";
    private static final String USER_NAME = "John Doe";
    private static final String RAW_PASSWORD = "SecureP@ss1";
    private static final String ENCODED_PASSWORD = "$2a$10$hashedPasswordValue123456";

    @Mock
    private AuthRepository authRepository;

    @Mock
    private MailService mailService;

    @Mock
    private OtpService otpService;

    @Mock
    private RoleService roleService;

    @Mock
    private UserService userService;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User activeUser;
    private TokenDto expectedTokens;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(USER_ID);
        activeUser.setEmail(USER_EMAIL);
        activeUser.setName(USER_NAME);
        activeUser.setStatus(UserStatus.ACTIVE);

        expectedTokens = TokenDto.builder()
                .accessToken("access-token-abc123")
                .refreshToken("refresh-token-xyz789")
                .build();
    }

    // ========== login ==========

    @Test
    @DisplayName("Should authenticate user and return tokens on successful login")
    void login_validCredentials_returnsTokenDto() {
        LoginRequest loginRequest = new LoginRequest(USER_EMAIL, RAW_PASSWORD);

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.isEnabled()).thenReturn(true);
        when(userDetails.getId()).thenReturn(USER_ID);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(userService.findByIdHelper(USER_ID)).thenReturn(activeUser);
        when(authTokenService.getAuthTokens(activeUser)).thenReturn(expectedTokens);

        TokenDto result = authService.login(loginRequest);

        assertNotNull(result);
        assertEquals("access-token-abc123", result.getAccessToken());
        assertEquals("refresh-token-xyz789", result.getRefreshToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService).findByIdHelper(USER_ID);
        verify(authTokenService).getAuthTokens(activeUser);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when account is not active")
    void login_accountNotActive_throwsBadCredentialsException() {
        LoginRequest loginRequest = new LoginRequest(USER_EMAIL, RAW_PASSWORD);

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.isEnabled()).thenReturn(false);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest));

        assertEquals("Account is not active", ex.getMessage());
        verify(authTokenService, never()).getAuthTokens(any());
    }

    @Test
    @DisplayName("Should propagate BadCredentialsException when authentication fails")
    void login_invalidCredentials_throwsBadCredentialsException() {
        LoginRequest loginRequest = new LoginRequest(USER_EMAIL, "wrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        verify(authTokenService, never()).getAuthTokens(any());
    }

    // ========== signup ==========

    @Test
    @DisplayName("Should create user, generate OTP and send email on successful signup")
    void signup_newUser_createsUserAndSendsOtp() {
        SignupRequest signupRequest = new SignupRequest(USER_NAME, USER_EMAIL, RAW_PASSWORD, RAW_PASSWORD);

        when(authRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        User savedUser = new User();
        savedUser.setId(USER_ID);
        savedUser.setEmail(USER_EMAIL);
        savedUser.setName(USER_NAME);
        when(authRepository.save(any(User.class))).thenReturn(savedUser);

        Otp otp = new Otp();
        otp.setValue(123456);
        when(otpService.createOtp(any(User.class), eq(OtpType.SIGNUP))).thenReturn(otp);

        authService.signup(signupRequest);

        verify(authRepository).findByEmail(USER_EMAIL);
        verify(passwordEncoder).encode(RAW_PASSWORD);
        verify(authRepository).save(any(User.class));
        verify(otpService).createOtp(any(User.class), eq(OtpType.SIGNUP));
        verify(mailService).sendEmailAsync(eq(USER_EMAIL), eq(AuthService.SIGNUP_MAIL_SUBJECT),
                eq(AuthService.SIGNUP_MAIL_TEXT + 123456));
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when email already exists during signup")
    void signup_existingEmail_throwsBadCredentialsException() {
        SignupRequest signupRequest = new SignupRequest(USER_NAME, USER_EMAIL, RAW_PASSWORD, RAW_PASSWORD);

        when(authRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(activeUser));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.signup(signupRequest));

        assertTrue(ex.getMessage().contains("Email already exists"));
        assertTrue(ex.getMessage().contains(UserStatus.ACTIVE.getValue()));
        verify(authRepository, never()).save(any());
        verify(otpService, never()).createOtp(any(), any());
    }

    // ========== resendSignupOtp ==========

    @Test
    @DisplayName("Should resend OTP when user is not yet verified")
    void resendSignupOtp_notVerifiedUser_resendsOtp() {
        OtpRequest request = new OtpRequest(USER_EMAIL);

        User notVerifiedUser = new User();
        notVerifiedUser.setEmail(USER_EMAIL);
        notVerifiedUser.setStatus(UserStatus.NOT_VERIFIED);
        when(authRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(notVerifiedUser));

        Otp otp = new Otp();
        otp.setValue(654321);
        when(otpService.getOrCreateOtp(notVerifiedUser, OtpType.SIGNUP)).thenReturn(otp);

        authService.resendSignupOtp(request);

        verify(otpService).getOrCreateOtp(notVerifiedUser, OtpType.SIGNUP);
        verify(mailService).sendEmailAsync(eq(USER_EMAIL), eq(AuthService.SIGNUP_MAIL_SUBJECT),
                eq(AuthService.SIGNUP_MAIL_TEXT + 654321));
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when email is not found during resend")
    void resendSignupOtp_emailNotFound_throwsBadCredentialsException() {
        OtpRequest request = new OtpRequest("nonexistent@example.com");

        when(authRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.resendSignupOtp(request));

        assertEquals("Email is invalid", ex.getMessage());
        verify(otpService, never()).getOrCreateOtp(any(), any());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when user is already active during resend")
    void resendSignupOtp_alreadyActiveUser_throwsBadCredentialsException() {
        OtpRequest request = new OtpRequest(USER_EMAIL);

        when(authRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(activeUser));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.resendSignupOtp(request));

        assertTrue(ex.getMessage().contains("Email already exists"));
        assertTrue(ex.getMessage().contains(UserStatus.ACTIVE.getValue()));
        verify(otpService, never()).getOrCreateOtp(any(), any());
    }

    // ========== verifySignupOtp ==========

    @Test
    @DisplayName("Should verify signup OTP, activate user, assign role and return tokens")
    void verifySignupOtp_validOtp_activatesUserAndReturnsTokens() {
        VerifySignupOtpRequest request = new VerifySignupOtpRequest(USER_EMAIL, 123456);

        User notVerifiedUser = new User();
        notVerifiedUser.setEmail(USER_EMAIL);
        notVerifiedUser.setStatus(UserStatus.NOT_VERIFIED);
        when(authRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(notVerifiedUser));

        doNothing().when(otpService).verifyOtp(OtpType.SIGNUP, 123456, USER_EMAIL);

        Role userRole = new Role();
        userRole.setName(RoleName.USER.getValue());
        when(roleService.findByName(RoleName.USER.getValue())).thenReturn(userRole);

        when(authRepository.save(notVerifiedUser)).thenReturn(notVerifiedUser);
        when(authTokenService.getAuthTokens(notVerifiedUser)).thenReturn(expectedTokens);

        TokenDto result = authService.verifySignupOtp(request);

        assertNotNull(result);
        assertEquals("access-token-abc123", result.getAccessToken());
        assertEquals(UserStatus.ACTIVE, notVerifiedUser.getStatus());
        assertTrue(notVerifiedUser.getRoles().contains(userRole));
        verify(otpService).verifyOtp(OtpType.SIGNUP, 123456, USER_EMAIL);
        verify(roleService).findByName(RoleName.USER.getValue());
        verify(authRepository).save(notVerifiedUser);
        verify(authTokenService).getAuthTokens(notVerifiedUser);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when user not found during signup OTP verification")
    void verifySignupOtp_userNotFound_throwsBadCredentialsException() {
        VerifySignupOtpRequest request = new VerifySignupOtpRequest("unknown@example.com", 123456);

        when(authRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.verifySignupOtp(request));

        assertEquals("User not found", ex.getMessage());
        verify(otpService, never()).verifyOtp(any(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when user is already verified during signup OTP verification")
    void verifySignupOtp_alreadyVerifiedUser_throwsBadCredentialsException() {
        VerifySignupOtpRequest request = new VerifySignupOtpRequest(USER_EMAIL, 123456);

        when(authRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(activeUser));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.verifySignupOtp(request));

        assertTrue(ex.getMessage().contains("Email already exists"));
        verify(otpService, never()).verifyOtp(any(), anyInt(), anyString());
    }

    // ========== forgetPassword ==========

    @Test
    @DisplayName("Should generate OTP and send forget password email")
    void forgetPassword_validEmail_sendsOtpEmail() {
        OtpRequest request = new OtpRequest(USER_EMAIL);

        when(authRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(activeUser));

        Otp otp = new Otp();
        otp.setValue(999888);
        when(otpService.getOrCreateOtp(activeUser, OtpType.FORGET)).thenReturn(otp);

        authService.forgetPassword(request);

        verify(otpService).getOrCreateOtp(activeUser, OtpType.FORGET);
        verify(mailService).sendEmailAsync(eq(USER_EMAIL), eq(AuthService.FORGET_PASSWORD_MAIL_SUBJECT),
                eq(AuthService.FORGET_PASSWORD_MAIL_TEXT + 999888));
    }

    @Test
    @DisplayName("Should throw ValidationException when email is invalid during forget password")
    void forgetPassword_invalidEmail_throwsValidationException() {
        OtpRequest request = new OtpRequest("invalid@example.com");

        when(authRepository.findByEmail("invalid@example.com")).thenReturn(Optional.empty());

        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.forgetPassword(request));

        assertEquals("Email is invalid", ex.getMessage());
        verify(otpService, never()).getOrCreateOtp(any(), any());
    }

    // ========== verifyForgetPasswordOtp ==========

    @Test
    @DisplayName("Should verify forget password OTP, update password and revoke tokens")
    void verifyForgetPasswordOtp_validOtp_updatesPasswordAndRevokesTokens() {
        VerifyForgetPasswordOtpRequest request =
                new VerifyForgetPasswordOtpRequest(USER_EMAIL, 111222, "NewP@ssword1", "NewP@ssword1");

        when(authRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(activeUser));
        doNothing().when(otpService).verifyOtp(OtpType.FORGET, 111222, USER_EMAIL);
        when(passwordEncoder.encode("NewP@ssword1")).thenReturn(ENCODED_PASSWORD);
        when(authRepository.save(activeUser)).thenReturn(activeUser);
        doNothing().when(authTokenService).revokeAllForUser(USER_ID);

        authService.verifyForgetPasswordOtp(request);

        verify(otpService).verifyOtp(OtpType.FORGET, 111222, USER_EMAIL);
        verify(passwordEncoder).encode("NewP@ssword1");
        verify(authRepository).save(activeUser);
        verify(authTokenService).revokeAllForUser(USER_ID);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when user not found during forget password OTP verification")
    void verifyForgetPasswordOtp_userNotFound_throwsBadCredentialsException() {
        VerifyForgetPasswordOtpRequest request =
                new VerifyForgetPasswordOtpRequest("unknown@example.com", 111222, "NewP@ss1", "NewP@ss1");

        when(authRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.verifyForgetPasswordOtp(request));

        assertEquals("User not found", ex.getMessage());
        verify(otpService, never()).verifyOtp(any(), anyInt(), anyString());
    }

    // ========== static constants ==========

    @Test
    @DisplayName("Should have correct forget password email subject and text constants")
    void staticConstants_forgetPassword_haveExpectedValues() {
        assertEquals("Forget password process", AuthService.FORGET_PASSWORD_MAIL_SUBJECT);
        assertTrue(AuthService.FORGET_PASSWORD_MAIL_TEXT.contains("One time password"));
    }

    @Test
    @DisplayName("Should have correct signup email subject and text constants")
    void staticConstants_signup_haveExpectedValues() {
        assertEquals("Signup process", AuthService.SIGNUP_MAIL_SUBJECT);
        assertTrue(AuthService.SIGNUP_MAIL_TEXT.contains("One time password"));
    }
}
