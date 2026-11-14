package com.example.ecom.common.serializer;

import com.example.ecom.common.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserStatusConverterTest {

    private UserStatusConverter converter;

    @BeforeEach
    void setUp() {
        converter = new UserStatusConverter();
    }

    @Test
    @DisplayName("Converts 'Active' string to UserStatus.ACTIVE")
    void convert_validValue_returnsUserStatus() {
        assertEquals(UserStatus.ACTIVE, converter.convert("Active"));
    }

    @Test
    @DisplayName("Converts enum name 'BANNED' to UserStatus.BANNED")
    void convert_enumName_returnsUserStatus() {
        assertEquals(UserStatus.BANNED, converter.convert("BANNED"));
    }

    @Test
    @DisplayName("Converts case-insensitive 'suspended' to UserStatus.SUSPENDED")
    void convert_caseInsensitive_returnsUserStatus() {
        assertEquals(UserStatus.SUSPENDED, converter.convert("suspended"));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException for invalid user status string")
    void convert_invalidValue_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert("NONEXISTENT_STATUS"));
    }
}
