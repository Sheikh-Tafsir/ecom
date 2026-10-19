package com.example.ecom.common.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void generateSecureRandomPassword_shouldReturnHighEntropyUniquePasswords() {
        Set<String> passwords = new HashSet<>();
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            String password = Utils.generateSecureRandomPassword();

            assertNotNull(password);
            assertFalse(password.isBlank());
            // 32 bytes in unpadded base64 is 43 chars
            assertTrue(password.length() >= 40, "Password length should be at least 40 characters");
            passwords.add(password);
        }

        // Ensure 100% uniqueness across 100 generations
        assertEquals(iterations, passwords.size(), "Every generated password should be completely unique");
    }

    @Test
    void isProductionEnvironment_whenProd_returnsTrue() {
        assertTrue(Utils.isEnvironmentProduction("prod"));
        assertTrue(Utils.isEnvironmentProduction("PROD"));
        assertTrue(Utils.isEnvironmentProduction(" prod "));
        assertTrue(Utils.isEnvironmentProduction("production"));
        assertTrue(Utils.isEnvironmentProduction("PRODUCTION"));
        assertTrue(Utils.isEnvironmentProduction(" production "));
    }

    @Test
    void isProductionEnvironment_whenNotProd_returnsFalse() {
        assertFalse(Utils.isEnvironmentProduction("dev"));
        assertFalse(Utils.isEnvironmentProduction("test"));
        assertFalse(Utils.isEnvironmentProduction("local"));
        assertFalse(Utils.isEnvironmentProduction("staging"));
        assertFalse(Utils.isEnvironmentProduction(""));
        assertFalse(Utils.isEnvironmentProduction("   "));
        assertFalse(Utils.isEnvironmentProduction(null));
    }
}
