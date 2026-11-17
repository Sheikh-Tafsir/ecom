package com.example.ecom.common.service.notification;

import com.example.ecom.common.enums.NotificationType;
import com.example.ecom.common.enums.Permission;
import com.example.ecom.common.dto.NotificationEvent;
import com.example.ecom.common.event.EventConstants;
import com.example.ecom.common.event.EventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.example.ecom.common.event.EventConstants.NOTIFICATION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private NotificationService notificationService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");

    @Test
    @DisplayName("sendToUser should publish NotificationEvent via EventPublisher")
    void sendToUser_publishesToPublisher() {
        notificationService.sendToUser(USER_ID, "Order shipped", NotificationType.INFO);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publish(eq(EventConstants.SOURCE_NOTIFICATION), eq(NOTIFICATION), eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();

        assertEquals(USER_ID, capturedEvent.recipientId());
        assertNull(capturedEvent.targetPermissions());
        assertEquals(NotificationType.INFO, capturedEvent.type());
        assertEquals("Order shipped", capturedEvent.message());
        assertNotNull(capturedEvent.createdAt());
    }

    @Test
    @DisplayName("sendToUser should do nothing if userId is null")
    void sendToUser_nullUserId_doesNothing() {
        notificationService.sendToUser(null, "Order shipped", NotificationType.INFO);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("sendToPermissions should publish NotificationEvent")
    void sendToPermissions_publishesToPublisher() {
        List<Permission> permissions = List.of(Permission.ADMIN_ACCESS, Permission.SUPER_ADMIN_ACCESS);

        notificationService.sendToPermissions(permissions, "Low stock", NotificationType.WARNING);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publish(eq(EventConstants.SOURCE_NOTIFICATION), eq(NOTIFICATION), eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();

        assertNull(capturedEvent.recipientId());
        assertEquals(permissions, capturedEvent.targetPermissions());
        assertEquals(NotificationType.WARNING, capturedEvent.type());
        assertEquals("Low stock", capturedEvent.message());
        assertNotNull(capturedEvent.createdAt());
    }

    @Test
    @DisplayName("sendToPermissions should do nothing if permissions list is empty or null")
    void sendToPermissions_emptyPermissions_doesNothing() {
        notificationService.sendToPermissions(Collections.emptyList(), "msg", NotificationType.INFO);
        notificationService.sendToPermissions(null, "msg", NotificationType.INFO);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("sendToAdmins should target ADMIN_ACCESS and SUPER_ADMIN_ACCESS permissions")
    void sendToAdmins_targetsAdminPermissions() {
        notificationService.sendToAdmins("5 orders pending", NotificationType.WARNING);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publish(eq(EventConstants.SOURCE_NOTIFICATION), eq(NOTIFICATION), eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();

        assertNull(capturedEvent.recipientId());
        assertEquals(List.of(Permission.SUPER_ADMIN_ACCESS, Permission.ADMIN_ACCESS), capturedEvent.targetPermissions());
        assertEquals(NotificationType.WARNING, capturedEvent.type());
        assertEquals("5 orders pending", capturedEvent.message());
    }

    @Test
    @DisplayName("sendToUser should handle publisher exception gracefully without throwing")
    void sendToUser_publisherFails_doesNotThrow() {
        doThrow(new RuntimeException("Redis connection refused"))
                .when(eventPublisher).publish(anyString(), anyString(), any());

        assertDoesNotThrow(() -> notificationService.sendToUser(USER_ID, "Order shipped", NotificationType.INFO));
    }

    @Test
    @DisplayName("sendToUser should skip when eventPublisher is null")
    void sendToUser_nullPublisher_doesNotThrow() {
        NotificationService service = new NotificationService(null);

        assertDoesNotThrow(() -> service.sendToUser(USER_ID, "Order shipped", NotificationType.INFO));
    }
}
