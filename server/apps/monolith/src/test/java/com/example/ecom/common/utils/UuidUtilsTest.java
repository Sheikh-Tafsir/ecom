package com.example.ecom.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UuidUtilsTest {

    @Test
    @DisplayName("Should generate RFC 9562 compliant UUIDv7")
    void testUuidV7Compliance() {
        UUID uuid = UuidUtils.generateUuidV7();

        assertNotNull(uuid);
        assertEquals(7, uuid.version(), "UUID version must be 7");
        assertEquals(2, uuid.variant(), "UUID variant must be 2 (RFC 4122/9562)");
    }

    @Test
    @DisplayName("Should generate monotonically increasing UUIDv7 in sequential order")
    void testUuidV7Monotonicity() {
        int count = 1000;
        UUID[] uuids = new UUID[count];

        for (int i = 0; i < count; i++) {
            uuids[i] = UuidUtils.generateUuidV7();
        }

        for (int i = 1; i < count; i++) {
            assertTrue(uuids[i - 1].compareTo(uuids[i]) < 0,
                    "Successive UUIDv7 instances must be strictly increasing: " + uuids[i - 1] + " < " + uuids[i]);
        }
    }
}
