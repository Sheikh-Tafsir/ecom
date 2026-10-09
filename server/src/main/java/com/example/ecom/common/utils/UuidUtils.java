package com.example.ecom.common.utils;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Utility for generating RFC 9562 compliant UUIDv7 identifiers.
 * UUIDv7 features a 48-bit timestamp in milliseconds followed by version,
 * sequence counter, variant, and cryptographically secure random bits.
 */
public final class UuidUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Object LOCK = new Object();
    private static long lastTimestamp = -1L;
    private static int sequence = 0;

    private UuidUtils() {}

    /**
     * Generates a monotonically increasing RFC 9562 UUIDv7.
     *
     * @return a new time-ordered UUIDv7 instance
     */
    public static UUID generateUuidV7() {
        long timestamp = System.currentTimeMillis();
        int seq;

        synchronized (LOCK) {
            if (timestamp > lastTimestamp) {
                lastTimestamp = timestamp;
                sequence = RANDOM.nextInt(1 << 8);
            } else {
                sequence++;
                if (sequence >= (1 << 12)) {
                    while (timestamp <= lastTimestamp) {
                        timestamp = System.currentTimeMillis();
                    }
                    lastTimestamp = timestamp;
                    sequence = 0;
                }
            }
            seq = sequence;
        }

        // 48 bits timestamp | 4 bits version 7 (0x7) | 12 bits sequence
        long msb = (timestamp << 16) | (0x7000L) | (seq & 0x0FFFL);

        // 2 bits variant (0x80) | 62 bits random entropy
        long lsb = RANDOM.nextLong();
        lsb = (lsb & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;

        return new UUID(msb, lsb);
    }
}
