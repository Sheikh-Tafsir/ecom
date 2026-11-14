package com.example.ecom.auth.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class OtpTypeTest {

    @ParameterizedTest
    @EnumSource(OtpType.class)
    @DisplayName("OtpType.fromValue round-trips for every constant via getValue()")
    void otpType_fromValue_roundTrips(OtpType otpType) {
        assertEquals(otpType, OtpType.fromValue(otpType.getValue()));
    }

    @ParameterizedTest
    @EnumSource(OtpType.class)
    @DisplayName("OtpType.fromValue works with enum name()")
    void otpType_fromName_works(OtpType otpType) {
        assertEquals(otpType, OtpType.fromValue(otpType.name()));
    }

    @Test
    @DisplayName("OtpType.fromValue returns null for null input")
    void otpType_fromValue_null_returnsNull() {
        assertNull(OtpType.fromValue(null));
    }

    @Test
    @DisplayName("OtpType.fromValue throws for invalid value")
    void otpType_fromValue_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> OtpType.fromValue("RESET"));
    }

    @Test
    @DisplayName("OtpType has SIGNUP and FORGET constants")
    void otpType_allConstants() {
        assertEquals(2, OtpType.values().length);
        assertNotNull(OtpType.valueOf("SIGNUP"));
        assertNotNull(OtpType.valueOf("FORGET"));
    }

    @Test
    @DisplayName("OtpType.getValue returns expected values")
    void otpType_getValue() {
        assertEquals("Signup", OtpType.SIGNUP.getValue());
        assertEquals("Forget", OtpType.FORGET.getValue());
    }
}
