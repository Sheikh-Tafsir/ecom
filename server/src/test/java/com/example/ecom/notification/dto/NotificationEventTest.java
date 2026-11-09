package com.example.ecom.notification.dto;

import com.example.ecom.common.enums.NotificationType;
import com.example.ecom.common.enums.Permission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationEventTest {

    @Test
    @DisplayName("Full canonical constructor sets all fields including createdAt")
    void constructor_allArgs_setsFields() {
        UUID recipientId = UUID.fromString("01927d76-097f-7afa-9aeb-304152637485");
        List<Permission> permissions = List.of(Permission.ADMIN_ACCESS);
        Instant timestamp = Instant.parse("2025-11-01T15:00:00Z");

        NotificationEvent event = new NotificationEvent(
                recipientId, permissions, NotificationType.SUCCESS, "Order delivered", timestamp
        );

        assertEquals(recipientId, event.recipientId());
        assertEquals(permissions, event.targetPermissions());
        assertEquals(NotificationType.SUCCESS, event.type());
        assertEquals("Order delivered", event.message());
        assertEquals(timestamp, event.createdAt());
    }

    @Test
    @DisplayName("Compact constructor auto-generates createdAt to now")
    void constructor_compactWithAutoTimestamp_setsCreatedAt() {
        UUID recipientId = UUID.fromString("01927d76-1a8f-7bfa-8aec-415263748596");
        Instant before = Instant.now();

        NotificationEvent event = new NotificationEvent(
                recipientId, List.of(Permission.USER), NotificationType.INFO, "Your order has been placed"
        );

        assertNotNull(event.createdAt());
        assertTrue(event.createdAt().compareTo(before) >= 0);
        assertEquals("Your order has been placed", event.message());
    }

    @Test
    @DisplayName("Null recipientId is allowed for broadcast notifications")
    void constructor_nullRecipient_allowed() {
        NotificationEvent event = new NotificationEvent(
                null, List.of(Permission.ADMIN_ACCESS), NotificationType.WARNING, "System alert"
        );

        assertNull(event.recipientId());
        assertEquals(NotificationType.WARNING, event.type());
    }

    @Test
    @DisplayName("Two equal events are equal")
    void equals_sameFields_returnsTrue() {
        UUID id = UUID.fromString("01927d76-2b9f-7cfa-7aed-526374859607");
        Instant ts = Instant.parse("2025-11-01T15:00:00Z");
        List<Permission> perms = List.of(Permission.USER);

        NotificationEvent e1 = new NotificationEvent(id, perms, NotificationType.ERROR, "Error msg", ts);
        NotificationEvent e2 = new NotificationEvent(id, perms, NotificationType.ERROR, "Error msg", ts);

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }
}
