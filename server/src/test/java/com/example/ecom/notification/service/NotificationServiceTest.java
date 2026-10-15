package com.example.ecom.notification.service;

import com.example.ecom.common.enums.NotificationType;
import com.example.ecom.notification.dto.NotificationEvent;
import com.example.ecom.notification.dto.NotificationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");

    @Test
    @DisplayName("sendToUser should serialize NotificationEvent and publish to ecom:notifications")
    void sendToUser_withRedis_publishesToChannel() throws JsonProcessingException {
        Instant now = Instant.now();
        NotificationResponse response = new NotificationResponse(NotificationType.INFO, "Order shipped", now);
        String expectedPayload = "{\"recipientType\":\"USER\",\"recipientId\":\"" + USER_ID + "\",\"type\":\"INFO\",\"message\":\"Order shipped\"}";

        when(objectMapper.writeValueAsString(any(NotificationEvent.class))).thenReturn(expectedPayload);

        notificationService.sendToUser(USER_ID, response);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(objectMapper).writeValueAsString(eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();

        assertEquals("USER", capturedEvent.recipientType());
        assertEquals(USER_ID, capturedEvent.recipientId());
        assertEquals(NotificationType.INFO, capturedEvent.type());
        assertEquals("Order shipped", capturedEvent.message());
        assertEquals(now, capturedEvent.createdAt());

        verify(stringRedisTemplate).convertAndSend(eq(NotificationService.NOTIFICATION_CHANNEL), eq(expectedPayload));
        assertEquals("ecom:notifications", NotificationService.NOTIFICATION_CHANNEL);
    }

    @Test
    @DisplayName("sendToUser with 2-arg NotificationResponse should default createdAt and publish")
    void sendToUser_withTwoArgNotificationResponse_defaultsCreatedAt() throws JsonProcessingException {
        NotificationResponse response = new NotificationResponse(NotificationType.SUCCESS, "Payment received");
        when(objectMapper.writeValueAsString(any(NotificationEvent.class))).thenReturn("{\"recipientType\":\"USER\"}");

        notificationService.sendToUser(USER_ID, response);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(objectMapper).writeValueAsString(eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();

        assertNotNull(capturedEvent.createdAt());
        assertEquals("USER", capturedEvent.recipientType());
        assertEquals(USER_ID, capturedEvent.recipientId());
        verify(stringRedisTemplate).convertAndSend(eq("ecom:notifications"), eq("{\"recipientType\":\"USER\"}"));
    }

    @Test
    @DisplayName("sendToUser should do nothing if userId is null")
    void sendToUser_nullUserId_doesNothing() {
        NotificationResponse response = new NotificationResponse(NotificationType.INFO, "Order shipped");

        notificationService.sendToUser(null, response);

        verifyNoInteractions(objectMapper);
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("sendToUser should do nothing if notificationResponse is null")
    void sendToUser_nullResponse_doesNothing() {
        notificationService.sendToUser(USER_ID, null);

        verifyNoInteractions(objectMapper);
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("sendToUser should log warning and not throw when stringRedisTemplate is null")
    void sendToUser_nullRedisTemplate_logsWarningAndDoesNotFail() {
        ReflectionTestUtils.setField(notificationService, "stringRedisTemplate", null);
        NotificationResponse response = new NotificationResponse(NotificationType.INFO, "Order shipped");

        assertDoesNotThrow(() -> notificationService.sendToUser(USER_ID, response));
        verifyNoInteractions(objectMapper);
    }

    @Test
    @DisplayName("sendToUser should handle ObjectMapper serialization failure gracefully")
    void sendToUser_serializationFails_doesNotThrow() throws JsonProcessingException {
        NotificationResponse response = new NotificationResponse(NotificationType.INFO, "Order shipped");
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization failed") {});

        assertDoesNotThrow(() -> notificationService.sendToUser(USER_ID, response));
        verify(stringRedisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("sendToUser should handle Redis convertAndSend failure gracefully")
    void sendToUser_redisFails_doesNotThrow() throws JsonProcessingException {
        NotificationResponse response = new NotificationResponse(NotificationType.INFO, "Order shipped");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"payload\":\"ok\"}");
        doThrow(new RuntimeException("Redis connection refused"))
                .when(stringRedisTemplate).convertAndSend(anyString(), anyString());

        assertDoesNotThrow(() -> notificationService.sendToUser(USER_ID, response));
    }

    @Test
    @DisplayName("sendToAdmins should serialize NotificationEvent and publish to ecom:notifications")
    void sendToAdmins_withRedis_publishesToChannel() throws JsonProcessingException {
        Instant now = Instant.now();
        NotificationResponse response = new NotificationResponse(NotificationType.WARNING, "Low stock alert", now);
        String expectedPayload = "{\"recipientType\":\"ADMIN\",\"recipientId\":null,\"type\":\"WARNING\",\"message\":\"Low stock alert\"}";

        when(objectMapper.writeValueAsString(any(NotificationEvent.class))).thenReturn(expectedPayload);

        notificationService.sendToAdmins(response);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(objectMapper).writeValueAsString(eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();

        assertEquals("ADMIN", capturedEvent.recipientType());
        assertNull(capturedEvent.recipientId());
        assertEquals(NotificationType.WARNING, capturedEvent.type());
        assertEquals("Low stock alert", capturedEvent.message());
        assertEquals(now, capturedEvent.createdAt());

        verify(stringRedisTemplate).convertAndSend(eq(NotificationService.NOTIFICATION_CHANNEL), eq(expectedPayload));
    }

    @Test
    @DisplayName("sendToAdmins should do nothing if notificationResponse is null")
    void sendToAdmins_nullResponse_doesNothing() {
        notificationService.sendToAdmins(null);

        verifyNoInteractions(objectMapper);
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("sendToAdmins should log warning and not throw when stringRedisTemplate is null")
    void sendToAdmins_nullRedisTemplate_logsWarningAndDoesNotFail() {
        ReflectionTestUtils.setField(notificationService, "stringRedisTemplate", null);
        NotificationResponse response = new NotificationResponse(NotificationType.WARNING, "Low stock");

        assertDoesNotThrow(() -> notificationService.sendToAdmins(response));
        verifyNoInteractions(objectMapper);
    }

    @Test
    @DisplayName("sendToAdmins should handle ObjectMapper serialization failure gracefully")
    void sendToAdmins_serializationFails_doesNotThrow() throws JsonProcessingException {
        NotificationResponse response = new NotificationResponse(NotificationType.WARNING, "Low stock");
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization error") {});

        assertDoesNotThrow(() -> notificationService.sendToAdmins(response));
        verify(stringRedisTemplate, never()).convertAndSend(anyString(), anyString());
    }
}
