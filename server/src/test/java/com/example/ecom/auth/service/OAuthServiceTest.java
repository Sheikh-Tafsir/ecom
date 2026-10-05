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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    private static final String CLIENT_ID = "712373259638-test.apps.googleusercontent.com";
    private static final String SECOND_CLIENT_ID = "712373259638-mobile.apps.googleusercontent.com";

    @Mock
    private WebClient webClient;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private AuthRepository authRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private TestableOAuthService oAuthService;

    // Subclass to override remote HTTP calls for deterministic unit testing
    static class TestableOAuthService extends OAuthService {
        GoogleUserDto stubTokenInfo;
        GoogleUserDto stubUserInfo;

        public TestableOAuthService(
                WebClient webClient,
                AuthTokenService authTokenService,
                AuthRepository authRepository,
                RoleService roleService,
                PasswordEncoder passwordEncoder,
                String googleClientId
        ) {
            super(webClient, authTokenService, authRepository, roleService, passwordEncoder, googleClientId);
        }

        @Override
        protected GoogleUserDto fetchTokenInfo(String token) {
            return stubTokenInfo;
        }

        @Override
        protected GoogleUserDto fetchUserInfo(String token) {
            return stubUserInfo;
        }
    }

    @BeforeEach
    void setUp() {
        oAuthService = new TestableOAuthService(
                webClient,
                authTokenService,
                authRepository,
                roleService,
                passwordEncoder,
                CLIENT_ID + ", " + SECOND_CLIENT_ID
        );
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when token is null or blank")
    void loginWithGoogle_whenTokenIsBlank_throwsBadCredentialsException() {
        assertThrows(BadCredentialsException.class, () -> oAuthService.loginWithGoogle(null));
        assertThrows(BadCredentialsException.class, () -> oAuthService.loginWithGoogle(new GoogleLoginRequest("")));
        assertThrows(BadCredentialsException.class, () -> oAuthService.loginWithGoogle(new GoogleLoginRequest("   ")));
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when google client ID is not configured on server")
    void loginWithGoogle_whenClientIdNotConfigured_throwsBadCredentialsException() {
        TestableOAuthService unconfiguredService = new TestableOAuthService(
                webClient, authTokenService, authRepository, roleService, passwordEncoder, ""
        );

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> unconfiguredService.loginWithGoogle(new GoogleLoginRequest("valid-token")));

        assertTrue(ex.getMessage().contains("not configured"));
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when Google rejects the token (null tokenInfo)")
    void loginWithGoogle_whenTokenInfoIsNull_throwsBadCredentialsException() {
        oAuthService.stubTokenInfo = null;

        assertThrows(BadCredentialsException.class,
                () -> oAuthService.loginWithGoogle(new GoogleLoginRequest("bad-token")));
    }

    @Test
    @DisplayName("Should reject token when audience (aud and azp) does not match configured client IDs")
    void loginWithGoogle_whenAudienceMismatches_throwsBadCredentialsException() {
        GoogleUserDto maliciousTokenInfo = new GoogleUserDto();
        maliciousTokenInfo.setAudience("malicious-app.apps.googleusercontent.com");
        maliciousTokenInfo.setAzp("malicious-app.apps.googleusercontent.com");
        maliciousTokenInfo.setEmail("victim@gmail.com");
        maliciousTokenInfo.setEmailVerified(true);
        oAuthService.stubTokenInfo = maliciousTokenInfo;

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> oAuthService.loginWithGoogle(new GoogleLoginRequest("stolen-token")));

        assertTrue(ex.getMessage().contains("audience mismatch"));
        verify(authRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject token when email is not verified by Google")
    void loginWithGoogle_whenEmailNotVerified_throwsBadCredentialsException() {
        GoogleUserDto tokenInfo = new GoogleUserDto();
        tokenInfo.setAudience(CLIENT_ID);
        tokenInfo.setEmail("unverified@gmail.com");
        tokenInfo.setEmailVerified(false);
        oAuthService.stubTokenInfo = tokenInfo;

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> oAuthService.loginWithGoogle(new GoogleLoginRequest("token")));

        assertTrue(ex.getMessage().contains("not verified"));
    }

    @Test
    @DisplayName("Should provision new user when user does not exist in database")
    void loginWithGoogle_whenNewUser_createsUserAndReturnsTokens() {
        GoogleUserDto tokenInfo = new GoogleUserDto();
        tokenInfo.setAudience(CLIENT_ID);
        tokenInfo.setEmail("newuser@gmail.com");
        tokenInfo.setEmailVerified(true);
        oAuthService.stubTokenInfo = tokenInfo;

        GoogleUserDto userInfo = new GoogleUserDto();
        userInfo.setName("New Google User");
        oAuthService.stubUserInfo = userInfo;

        when(authRepository.findByEmail("newuser@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedSecurePassword");

        Role userRole = new Role();
        userRole.setName(RoleName.USER.getValue());
        when(roleService.findByName(RoleName.USER.getValue())).thenReturn(userRole);

        when(authRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenDto expectedTokens = new TokenDto("access-token-123", "refresh-token-456");
        when(authTokenService.getAuthTokens(any(User.class))).thenReturn(expectedTokens);

        TokenDto result = oAuthService.loginWithGoogle(new GoogleLoginRequest("valid-access-token"));

        assertNotNull(result);
        assertEquals(expectedTokens, result);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(authRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("newuser@gmail.com", savedUser.getEmail());
        assertEquals("New Google User", savedUser.getName());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
        assertEquals("$2a$10$hashedSecurePassword", savedUser.getPassword());
        assertTrue(savedUser.getRoles().contains(userRole));
    }

    @Test
    @DisplayName("Should authenticate existing user without creating a new database record")
    void loginWithGoogle_whenExistingUser_returnsTokensWithoutCreatingUser() {
        GoogleUserDto tokenInfo = new GoogleUserDto();
        tokenInfo.setAzp(SECOND_CLIENT_ID); // Test second allowed client ID
        tokenInfo.setEmail("existing@gmail.com");
        tokenInfo.setEmailVerified(true);
        tokenInfo.setName("Existing User");
        oAuthService.stubTokenInfo = tokenInfo;

        User existingUser = new User();
        existingUser.setId(10L);
        existingUser.setEmail("existing@gmail.com");
        existingUser.setName("Existing User");
        existingUser.setStatus(UserStatus.ACTIVE);

        when(authRepository.findByEmail("existing@gmail.com")).thenReturn(Optional.of(existingUser));

        TokenDto expectedTokens = new TokenDto("access-token-789", "refresh-token-012");
        when(authTokenService.getAuthTokens(existingUser)).thenReturn(expectedTokens);

        TokenDto result = oAuthService.loginWithGoogle(new GoogleLoginRequest("valid-token"));

        assertNotNull(result);
        assertEquals(expectedTokens, result);
        verify(authRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should verify JWT format recognition")
    void isJwt_correctlyIdentifiesJwtVsAccessToken() {
        OAuthService realService = new OAuthService(webClient, authTokenService, authRepository, roleService, passwordEncoder, CLIENT_ID);

        assertTrue(realService.isJwt("header.payload.signature"));
        assertTrue(realService.isJwt("eyJhbGciOiJSUzI1NiIsImtpZCI6IjEifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20ifQ.abcdef123456"));

        assertFalse(realService.isJwt("ya29.a0AWY7Ckm123456789"));
        assertFalse(realService.isJwt("opaque_token_without_dots"));
        assertFalse(realService.isJwt("only.one.dot.too.many.dots"));
        assertFalse(realService.isJwt(null));
    }
}
