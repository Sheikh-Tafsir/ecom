package com.example.ecom.auth.service;

import com.example.ecom.auth.dto.Otp;
import com.example.ecom.auth.enums.OtpType;
import com.example.ecom.user.user.model.User;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static com.example.ecom.common.utils.CacheConstants.CACHE_OTPS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final String USER_EMAIL = "jane.smith@example.com";
    private static final UUID USER_ID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f23456789012");

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private OtpService otpService;

    private User testUser;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(cacheManager);
        ReflectionTestUtils.setField(otpService, "maxTries", 3);

        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setEmail(USER_EMAIL);
    }

    // ========== createOtp ==========

    @Test
    @DisplayName("Should create OTP with correct email, type and 6-digit value")
    void createOtp_validUser_createsOtpWithCorrectFields() {
        when(cacheManager.getCache(CACHE_OTPS)).thenReturn(cache);

        Otp otp = otpService.createOtp(testUser, OtpType.SIGNUP);

        assertNotNull(otp);
        assertEquals(USER_EMAIL, otp.getEmail());
        assertEquals(OtpType.SIGNUP, otp.getType());
        assertTrue(otp.getValue() >= 100000 && otp.getValue() <= 999999,
                "OTP value should be a 6-digit number");
        verify(cache).put(eq("SIGNUP:" + USER_EMAIL), eq(otp));
    }

    @Test
    @DisplayName("Should create FORGET type OTP and save to cache")
    void createOtp_forgetType_createsOtpWithForgetType() {
        when(cacheManager.getCache(CACHE_OTPS)).thenReturn(cache);

        Otp otp = otpService.createOtp(testUser, OtpType.FORGET);

        assertEquals(OtpType.FORGET, otp.getType());
        verify(cache).put(eq("FORGET:" + USER_EMAIL), eq(otp));
    }

    @Test
    @DisplayName("Should handle null cache gracefully when creating OTP")
    void createOtp_nullCache_doesNotThrow() {
        when(cacheManager.getCache(CACHE_OTPS)).thenReturn(null);

        Otp otp = otpService.createOtp(testUser, OtpType.SIGNUP);

        assertNotNull(otp);
        assertEquals(USER_EMAIL, otp.getEmail());
    }

    // ========== getOrCreateOtp ==========

    @Test
    @DisplayName("Should create new OTP when no existing OTP is found")
    void getOrCreateOtp_noExistingOtp_createsNewOtp() {
        when(cacheManager.getCache(CACHE_OTPS)).thenReturn(cache);
        when(cache.get("SIGNUP:" + USER_EMAIL, Otp.class)).thenReturn(null);

        Otp otp = otpService.getOrCreateOtp(testUser, OtpType.SIGNUP);

        assertNotNull(otp);
        assertEquals(OtpType.SIGNUP, otp.getType());
        verify(cache).put(eq("SIGNUP:" + USER_EMAIL), eq(otp));
    }

    @Test
    @DisplayName("Should throw ValidationException when OTP already exists for user")
    void getOrCreateOtp_existingOtp_throwsValidationException() {
        when(cacheManager.getCache(CACHE_OTPS)).thenReturn(cache);

        Otp existingOtp = new Otp();
        existingOtp.setEmail(USER_EMAIL);
        existingOtp.setType(OtpType.SIGNUP);
        existingOtp.setValue(555555);
        when(cache.get("SIGNUP:" + USER_EMAIL, Otp.class)).thenReturn(existingOtp);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> otpService.getOrCreateOtp(testUser, OtpType.SIGNUP));

        assertTrue(ex.getMessage().contains("SIGNUP"));
        assertTrue(ex.getMessage().contains(USER_EMAIL));
    }

    // ========== verifyOtp ==========

    @Test
    @DisplayName("Should verify OTP successfully and delete it from cache")
    void verifyOtp_correctOtp_deletesFromCache() {
        when(cacheManager.getCache(CACHE_OTPS)).thenReturn(cache);

        Otp storedOtp = new Otp();
        storedOtp.setEmail(USER_EMAIL);
        storedOtp.setType(OtpType.SIGNUP);
        storedOtp.setValue(123456);
        storedOtp.setTries(0);
        when(cache.get("SIGNUP:" + USER_EMAIL, Otp.class)).thenReturn(storedOtp);

        assertDoesNotThrow(() -> otpService.verifyOtp(OtpType.SIGNUP, 123456, USER_EMAIL));

        verify(cache).evict("SIGNUP:" + USER_EMAIL);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when no OTP is found")
    void verifyOtp_noOtpFound_throwsBadCredentialsException() {
        when(cacheManager.getCache(CACHE_OTPS)).thenReturn(cache);
        when(cache.get("SIGNUP:" + USER_EMAIL, Otp.class)).thenReturn(null);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> otpService.verifyOtp(OtpType.SIGNUP, 123456, USER_EMAIL));

        assertEquals("No OTP found", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when max tries already reached")
    void verifyOtp_maxTriesReached_throwsBadCredentialsExceptionAndDeletesOtp() {
        when(cacheManager.getCache(CACHE_OTPS)).thenReturn(cache);

        Otp storedOtp = new Otp();
        storedOtp.setEmail(USER_EMAIL);
        storedOtp.setType(OtpType.FORGET);
        storedOtp.setValue(123456);
        storedOtp.setTries(3);
        when(cache.get("FORGET:" + USER_EMAIL, Otp.class)).thenReturn(storedOtp);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> otpService.verifyOtp(OtpType.FORGET, 999999, USER_EMAIL));

        assertTrue(ex.getMessage().contains("Max tries attempted"));
        verify(cache).evict("FORGET:" + USER_EMAIL);
    }

    @Test
    @DisplayName("Should increment tries and throw BadCredentialsException on wrong OTP")
    void verifyOtp_wrongOtp_incrementsTriesAndThrows() {
        when(cacheManager.getCache(CACHE_OTPS)).thenReturn(cache);

        Otp storedOtp = new Otp();
        storedOtp.setEmail(USER_EMAIL);
        storedOtp.setType(OtpType.SIGNUP);
        storedOtp.setValue(123456);
        storedOtp.setTries(0);
        when(cache.get("SIGNUP:" + USER_EMAIL, Otp.class)).thenReturn(storedOtp);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> otpService.verifyOtp(OtpType.SIGNUP, 999999, USER_EMAIL));

        assertEquals("Invalid OTP", ex.getMessage());
        assertEquals(1, storedOtp.getTries());
        verify(cache).put(eq("SIGNUP:" + USER_EMAIL), eq(storedOtp));
    }

    @Test
    @DisplayName("Should delete OTP and throw max tries when wrong OTP causes tries to reach max")
    void verifyOtp_wrongOtpReachesMaxTries_deletesOtpAndThrows() {
        when(cacheManager.getCache(CACHE_OTPS)).thenReturn(cache);

        Otp storedOtp = new Otp();
        storedOtp.setEmail(USER_EMAIL);
        storedOtp.setType(OtpType.SIGNUP);
        storedOtp.setValue(123456);
        storedOtp.setTries(2); // one more wrong try will hit maxTries=3
        when(cache.get("SIGNUP:" + USER_EMAIL, Otp.class)).thenReturn(storedOtp);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> otpService.verifyOtp(OtpType.SIGNUP, 999999, USER_EMAIL));

        assertTrue(ex.getMessage().contains("Max tries attempted"));
        verify(cache).evict("SIGNUP:" + USER_EMAIL);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when cache is null and no OTP can be found")
    void verifyOtp_nullCache_throwsNoOtpFound() {
        when(cacheManager.getCache(CACHE_OTPS)).thenReturn(null);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> otpService.verifyOtp(OtpType.SIGNUP, 123456, USER_EMAIL));

        assertEquals("No OTP found", ex.getMessage());
    }
}
