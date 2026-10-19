package com.example.ecom.notification.service;

import com.example.ecom.common.enums.NotificationType;
import com.example.ecom.common.enums.Permission;
import com.example.ecom.notification.dto.NotificationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private NotificationService notificationService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");

    @Test
    @DisplayName("sendToUser should serialize NotificationEvent and publish via NotificationPublisher")
    void sendToUser_publishesToPublisher() throws JsonProcessingException {
        String expectedPayload = "{\"recipientId\":\"" + USER_ID + "\",\"type\":\"INFO\",\"message\":\"Order shipped\"}";

        when(objectMapper.writeValueAsString(any(NotificationEvent.class))).thenReturn(expectedPayload);

        notificationService.sendToUser(USER_ID, "Order shipped", NotificationType.INFO);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(objectMapper).writeValueAsString(eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();

        assertEquals(USER_ID, capturedEvent.recipientId());
        assertNull(capturedEvent.targetPermissions());
        assertEquals(NotificationType.INFO, capturedEvent.type());
        assertEquals("Order shipped", capturedEvent.message());
        assertNotNull(capturedEvent.createdAt());

        verify(notificationPublisher).publish(expectedPayload);
    }

    @Test
    @DisplayName("sendToUser should do nothing if userId is null")
    void sendToUser_nullUserId_doesNothing() {
        notificationService.sendToUser(null, "Order shipped", NotificationType.INFO);

        verifyNoInteractions(objectMapper);
        verifyNoInteractions(notificationPublisher);
    }

    @Test
    @DisplayName("sendToPermissions should serialize NotificationEvent and publish")
    void sendToPermissions_publishesToPublisher() throws JsonProcessingException {
        List<Permission> permissions = List.of(Permission.ADMIN_ACCESS, Permission.SUPER_ADMIN_ACCESS);
        String expectedPayload = "{\"targetPermissions\":[\"admin:access\",\"super_admin:access\"],\"type\":\"WARNING\",\"message\":\"Low stock\"}";

        when(objectMapper.writeValueAsString(any(NotificationEvent.class))).thenReturn(expectedPayload);

        notificationService.sendToPermissions(permissions, "Low stock", NotificationType.WARNING);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(objectMapper).writeValueAsString(eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();

        assertNull(capturedEvent.recipientId());
        assertEquals(permissions, capturedEvent.targetPermissions());
        assertEquals(NotificationType.WARNING, capturedEvent.type());
        assertEquals("Low stock", capturedEvent.message());
        assertNotNull(capturedEvent.createdAt());

        verify(notificationPublisher).publish(expectedPayload);
    }

    @Test
    @DisplayName("sendToPermissions should do nothing if permissions list is empty or null")
    void sendToPermissions_emptyPermissions_doesNothing() {
        notificationService.sendToPermissions(Collections.emptyList(), "msg", NotificationType.INFO);
        notificationService.sendToPermissions(null, "msg", NotificationType.INFO);

        verifyNoInteractions(objectMapper);
        verifyNoInteractions(notificationPublisher);
    }

    @Test
    @DisplayName("sendToAdmins should target ADMIN_ACCESS and SUPER_ADMIN_ACCESS permissions")
    void sendToAdmins_targetsAdminPermissions() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any(NotificationEvent.class))).thenReturn("{\"payload\":\"ok\"}");

        notificationService.sendToAdmins("5 orders pending", NotificationType.WARNING);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(objectMapper).writeValueAsString(eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();

        assertNull(capturedEvent.recipientId());
        assertEquals(List.of(Permission.SUPER_ADMIN_ACCESS, Permission.ADMIN_ACCESS), capturedEvent.targetPermissions());
        assertEquals(NotificationType.WARNING, capturedEvent.type());
        assertEquals("5 orders pending", capturedEvent.message());

        verify(notificationPublisher).publish("{\"payload\":\"ok\"}");
    }

    @Test
    @DisplayName("sendToUser should handle publisher exception gracefully without throwing")
    void sendToUser_publisherFails_doesNotThrow() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"payload\":\"ok\"}");
        doThrow(new RuntimeException("Redis connection refused"))
                .when(notificationPublisher).publish(anyString());

        assertDoesNotThrow(() -> notificationService.sendToUser(USER_ID, "Order shipped", NotificationType.INFO));
    }

    @Test
    @DisplayName("sendToUser should handle ObjectMapper serialization failure gracefully")
    void sendToUser_serializationFails_doesNotThrow() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization failed") {});

        assertDoesNotThrow(() -> notificationService.sendToUser(USER_ID, "Order shipped", NotificationType.INFO));
        verify(notificationPublisher, never()).publish(anyString());
    }
}
