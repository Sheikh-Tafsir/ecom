package com.example.ecom.auth.dto;

import com.example.ecom.auth.enums.OtpType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthDtoTest {

    @Nested
    @DisplayName("LoginRequest Tests")
    class LoginRequestTest {

        @Test
        @DisplayName("Record constructor sets email and password")
        void constructor_validArgs_setsFields() {
            LoginRequest request = new LoginRequest("user@example.com", "securePass1");

            assertEquals("user@example.com", request.email());
            assertEquals("securePass1", request.password());
        }
    }

    @Nested
    @DisplayName("SignupRequest Tests")
    class SignupRequestTest {

        @Test
        @DisplayName("Record constructor sets all fields")
        void constructor_validArgs_setsFields() {
            SignupRequest request = new SignupRequest(
                    "John Doe", "john@example.com", "password1", "password1"
            );

            assertEquals("John Doe", request.name());
            assertEquals("john@example.com", request.email());
            assertEquals("password1", request.password());
            assertEquals("password1", request.confirmPassword());
        }

        @Test
        @DisplayName("Two equal records are equal")
        void equals_sameFields_returnsTrue() {
            SignupRequest r1 = new SignupRequest("Name", "e@e.com", "pass1234", "pass1234");
            SignupRequest r2 = new SignupRequest("Name", "e@e.com", "pass1234", "pass1234");

            assertEquals(r1, r2);
        }
    }

    @Nested
    @DisplayName("OtpRequest Tests")
    class OtpRequestTest {

        @Test
        @DisplayName("Record constructor sets email")
        void constructor_validArgs_setsFields() {
            OtpRequest request = new OtpRequest("test@example.com");
            assertEquals("test@example.com", request.email());
        }
    }

    @Nested
    @DisplayName("Otp Tests")
    class OtpTest {

        @Test
        @DisplayName("All-args constructor sets all fields")
        void allArgsConstructor_validArgs_setsFields() {
            Otp otp = new Otp("user@example.com", OtpType.SIGNUP, 123456, 0);

            assertEquals("user@example.com", otp.getEmail());
            assertEquals(OtpType.SIGNUP, otp.getType());
            assertEquals(123456, otp.getValue());
            assertEquals(0, otp.getTries());
        }

        @Test
        @DisplayName("No-args constructor creates empty instance")
        void noArgsConstructor_createsEmpty() {
            Otp otp = new Otp();
            assertNull(otp.getEmail());
            assertNull(otp.getType());
            assertEquals(0, otp.getValue());
            assertEquals(0, otp.getTries());
        }

        @Test
        @DisplayName("Setters update all fields")
        void setters_allFields_updateCorrectly() {
            Otp otp = new Otp();
            otp.setEmail("forget@example.com");
            otp.setType(OtpType.FORGET);
            otp.setValue(654321);
            otp.setTries(3);

            assertEquals("forget@example.com", otp.getEmail());
            assertEquals(OtpType.FORGET, otp.getType());
            assertEquals(654321, otp.getValue());
            assertEquals(3, otp.getTries());
        }
    }

    @Nested
    @DisplayName("TokenDto Tests")
    class TokenDtoTest {

        @Test
        @DisplayName("Builder creates TokenDto with all fields")
        void builder_validArgs_setsFields() {
            TokenDto token = TokenDto.builder()
                    .accessToken("access-jwt-token")
                    .refreshToken("refresh-jwt-token")
                    .build();

            assertEquals("access-jwt-token", token.getAccessToken());
            assertEquals("refresh-jwt-token", token.getRefreshToken());
        }

        @Test
        @DisplayName("No-args constructor creates empty instance")
        void noArgsConstructor_createsEmpty() {
            TokenDto token = new TokenDto();
            assertNull(token.getAccessToken());
            assertNull(token.getRefreshToken());
        }

        @Test
        @DisplayName("All-args constructor sets fields")
        void allArgsConstructor_setsFields() {
            TokenDto token = new TokenDto("access", "refresh");
            assertEquals("access", token.getAccessToken());
            assertEquals("refresh", token.getRefreshToken());
        }

        @Test
        @DisplayName("Setters update fields")
        void setters_updateCorrectly() {
            TokenDto token = new TokenDto();
            token.setAccessToken("new-access");
            token.setRefreshToken("new-refresh");

            assertEquals("new-access", token.getAccessToken());
            assertEquals("new-refresh", token.getRefreshToken());
        }
    }

    @Nested
    @DisplayName("VerifySignupOtpRequest Tests")
    class VerifySignupOtpRequestTest {

        @Test
        @DisplayName("Record constructor sets email and otp")
        void constructor_validArgs_setsFields() {
            VerifySignupOtpRequest request = new VerifySignupOtpRequest("user@example.com", 123456);

            assertEquals("user@example.com", request.email());
            assertEquals(123456, request.otp());
        }
    }

    @Nested
    @DisplayName("VerifyForgetPasswordOtpRequest Tests")
    class VerifyForgetPasswordOtpRequestTest {

        @Test
        @DisplayName("Record constructor sets all fields")
        void constructor_validArgs_setsFields() {
            VerifyForgetPasswordOtpRequest request = new VerifyForgetPasswordOtpRequest(
                    "user@example.com", 987654, "newPass123", "newPass123"
            );

            assertEquals("user@example.com", request.email());
            assertEquals(987654, request.otp());
            assertEquals("newPass123", request.password());
            assertEquals("newPass123", request.confirmPassword());
        }
    }

    @Nested
    @DisplayName("GoogleLoginRequest Tests")
    class GoogleLoginRequestTest {

        @Test
        @DisplayName("Record constructor sets token")
        void constructor_validArgs_setsFields() {
            GoogleLoginRequest request = new GoogleLoginRequest("google-oauth-token-xyz");
            assertEquals("google-oauth-token-xyz", request.token());
        }
    }

    @Nested
    @DisplayName("GoogleUserDto Tests")
    class GoogleUserDtoTest {

        @Test
        @DisplayName("Setters and getters work for all fields")
        void settersGetters_allFields_workCorrectly() {
            GoogleUserDto dto = new GoogleUserDto();
            dto.setSub("google-sub-id-12345");
            dto.setEmail("google.user@gmail.com");
            dto.setEmailVerified(true);
            dto.setName("Google User");
            dto.setAudience("client-id-123.apps.googleusercontent.com");
            dto.setAzp("client-id-123.apps.googleusercontent.com");

            assertEquals("google-sub-id-12345", dto.getSub());
            assertEquals("google.user@gmail.com", dto.getEmail());
            assertTrue(dto.isEmailVerified());
            assertEquals("Google User", dto.getName());
            assertEquals("client-id-123.apps.googleusercontent.com", dto.getAudience());
            assertEquals("client-id-123.apps.googleusercontent.com", dto.getAzp());
        }

        @Test
        @DisplayName("matchesAudience returns true when audience matches allowed client ids")
        void matchesAudience_audienceMatches_returnsTrue() {
            GoogleUserDto dto = new GoogleUserDto();
            dto.setAudience("client-id-1");

            assertTrue(dto.matchesAudience(List.of("client-id-1", "client-id-2")));
        }

        @Test
        @DisplayName("matchesAudience returns true when azp matches allowed client ids")
        void matchesAudience_azpMatches_returnsTrue() {
            GoogleUserDto dto = new GoogleUserDto();
            dto.setAzp("client-id-2");

            assertTrue(dto.matchesAudience(List.of("client-id-1", "client-id-2")));
        }

        @Test
        @DisplayName("matchesAudience returns false when neither audience nor azp matches")
        void matchesAudience_noMatch_returnsFalse() {
            GoogleUserDto dto = new GoogleUserDto();
            dto.setAudience("client-id-unknown");
            dto.setAzp("client-id-also-unknown");

            assertFalse(dto.matchesAudience(List.of("client-id-1", "client-id-2")));
        }

        @Test
        @DisplayName("matchesAudience returns false for empty allowed client ids")
        void matchesAudience_emptyAllowed_returnsFalse() {
            GoogleUserDto dto = new GoogleUserDto();
            dto.setAudience("client-id-1");

            assertFalse(dto.matchesAudience(List.of()));
        }

        @Test
        @DisplayName("matchesAudience returns false for null allowed client ids")
        void matchesAudience_nullAllowed_returnsFalse() {
            GoogleUserDto dto = new GoogleUserDto();
            dto.setAudience("client-id-1");

            assertFalse(dto.matchesAudience(null));
        }

        @Test
        @DisplayName("matchesAudience returns false when audience and azp are null")
        void matchesAudience_nullAudienceAndAzp_returnsFalse() {
            GoogleUserDto dto = new GoogleUserDto();
            dto.setAudience(null);
            dto.setAzp(null);

            assertFalse(dto.matchesAudience(List.of("client-id-1")));
        }
    }
}
