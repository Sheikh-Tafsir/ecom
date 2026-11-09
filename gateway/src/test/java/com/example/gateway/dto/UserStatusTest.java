package com.example.gateway.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class UserStatusTest {

    @Test
    @DisplayName("fromValue returns ACTIVE when given 'Active' (display value)")
    void fromValue_activeDisplayValue_returnsActive() {
        assertEquals(UserStatus.ACTIVE, UserStatus.fromValue("Active"));
    }

    @Test
    @DisplayName("fromValue returns ACTIVE when given 'ACTIVE' (enum name)")
    void fromValue_activeEnumName_returnsActive() {
        assertEquals(UserStatus.ACTIVE, UserStatus.fromValue("ACTIVE"));
    }

    @Test
    @DisplayName("fromValue is case-insensitive for display values")
    void fromValue_caseInsensitiveDisplayValue_returnsCorrectStatus() {
        assertEquals(UserStatus.NOT_VERIFIED, UserStatus.fromValue("not verified"));
        assertEquals(UserStatus.SUSPENDED, UserStatus.fromValue("suspended"));
        assertEquals(UserStatus.BANNED, UserStatus.fromValue("BANNED"));
    }

    @Test
    @DisplayName("fromValue is case-insensitive for enum names")
    void fromValue_caseInsensitiveEnumName_returnsCorrectStatus() {
        assertEquals(UserStatus.NOT_VERIFIED, UserStatus.fromValue("not_verified"));
        assertEquals(UserStatus.INACTIVE, UserStatus.fromValue("inactive"));
        assertEquals(UserStatus.DELETED, UserStatus.fromValue("deleted"));
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("fromValue returns null when given null input")
    void fromValue_nullInput_returnsNull(String value) {
        assertNull(UserStatus.fromValue(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "UNKNOWN", "InvalidStatus", "   ", "ACTIVEX"})
    @DisplayName("fromValue throws IllegalArgumentException for invalid values")
    void fromValue_invalidValue_throwsIllegalArgumentException(String value) {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> UserStatus.fromValue(value)
        );
        assertTrue(ex.getMessage().contains("Invalid User Status"));
    }

    @ParameterizedTest
    @EnumSource(UserStatus.class)
    @DisplayName("All enum constants round-trip via fromValue using their display value")
    void fromValue_allConstants_roundTripByDisplayValue(UserStatus status) {
        assertEquals(status, UserStatus.fromValue(status.getValue()));
    }

    @ParameterizedTest
    @EnumSource(UserStatus.class)
    @DisplayName("All enum constants round-trip via fromValue using their enum name")
    void fromValue_allConstants_roundTripByEnumName(UserStatus status) {
        assertEquals(status, UserStatus.fromValue(status.name()));
    }

    @Test
    @DisplayName("getValue returns the display value for each enum constant")
    void getValue_allConstants_returnsDisplayValue() {
        assertEquals("Not Verified", UserStatus.NOT_VERIFIED.getValue());
        assertEquals("Active", UserStatus.ACTIVE.getValue());
        assertEquals("Suspended", UserStatus.SUSPENDED.getValue());
        assertEquals("Inactive", UserStatus.INACTIVE.getValue());
        assertEquals("Deleted", UserStatus.DELETED.getValue());
        assertEquals("Banned", UserStatus.BANNED.getValue());
    }

    @Test
    @DisplayName("valueOf works for all enum constant names")
    void valueOf_allConstants_returnsCorrectEnum() {
        assertEquals(UserStatus.NOT_VERIFIED, UserStatus.valueOf("NOT_VERIFIED"));
        assertEquals(UserStatus.ACTIVE, UserStatus.valueOf("ACTIVE"));
        assertEquals(UserStatus.SUSPENDED, UserStatus.valueOf("SUSPENDED"));
        assertEquals(UserStatus.INACTIVE, UserStatus.valueOf("INACTIVE"));
        assertEquals(UserStatus.DELETED, UserStatus.valueOf("DELETED"));
        assertEquals(UserStatus.BANNED, UserStatus.valueOf("BANNED"));
    }

    @Test
    @DisplayName("values() returns all six enum constants")
    void values_returnsAllConstants() {
        assertEquals(6, UserStatus.values().length);
    }
}
