package com.example.ecom.notification.controller;

import com.example.ecom.common.dto.ApiResponse;
import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = mock(CustomUserDetails.class);
    }

    @Test
    @DisplayName("getSseTicket should return 200 with ticket when user is authenticated")
    void getSseTicket_authenticated_returnsTicket() {
        when(notificationService.generateSseAuthToken(userDetails)).thenReturn("test-ticket-uuid");

        ResponseEntity<ApiResponse<String>> response = notificationController.getSseTicket(userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("test-ticket-uuid", response.getBody().getData());
        verify(notificationService).generateSseAuthToken(userDetails);
    }

    @Test
    @DisplayName("subscribe should return 401 UNAUTHORIZED when userDetails is null")
    void subscribe_unauthenticated_returns401() {
        ResponseEntity<SseEmitter> response = notificationController.subscribe(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("subscribe should return 200 with SseEmitter when user is authenticated")
    void subscribe_authenticated_returns200WithEmitter() {
        SseEmitter emitter = new SseEmitter();
        when(notificationService.subscribe(userDetails)).thenReturn(emitter);

        ResponseEntity<SseEmitter> response = notificationController.subscribe(userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(emitter, response.getBody());
        verify(notificationService).subscribe(userDetails);
    }
}
