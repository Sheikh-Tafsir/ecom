package com.example.ecom.notification.service;

import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.enums.NotificationType;
import com.example.ecom.common.enums.Permission;
import com.example.ecom.common.model.User;
import com.example.ecom.common.service.JwtService;
import com.example.ecom.notification.dto.NotificationEvent;
import com.example.ecom.notification.dto.NotificationResponse;
import com.example.ecom.user.user.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.example.ecom.common.utils.CacheConstants.CACHE_SSE_TICKETS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        when(cacheManager.getCache(CACHE_SSE_TICKETS)).thenReturn(cache);
        ReflectionTestUtils.setField(notificationService, "stringRedisTemplate", stringRedisTemplate);
    }

    @Test
    @DisplayName("generateSseAuthToken should generate token, store in cache with ticket, and return ticket")
    void generateSseAuthToken_success() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(10L);

        User user = new User();
        user.setId(10L);
        when(userService.findByIdHelper(10L)).thenReturn(user);
        when(jwtService.generateSseAccessToken(user)).thenReturn("sse-jwt-token");

        String ticket = notificationService.generateSseAuthToken(userDetails);

        assertNotNull(ticket);
        assertFalse(ticket.isBlank());
        verify(cache).put(eq(ticket), eq("sse-jwt-token"));
    }

    @Test
    @DisplayName("getAndDeleteTokenByTicket should return token and evict ticket from cache")
    void getAndDeleteTokenByTicket_found_evictsAndReturnsToken() {
        when(cache.get("test-ticket", String.class)).thenReturn("sse-jwt-token");

        String token = notificationService.getAndDeleteTokenByTicket("test-ticket");

        assertEquals("sse-jwt-token", token);
        verify(cache).evict("test-ticket");
    }

    @Test
    @DisplayName("getAndDeleteTokenByTicket should return null when ticket not in cache")
    void getAndDeleteTokenByTicket_notFound_returnsNull() {
        when(cache.get("unknown-ticket", String.class)).thenReturn(null);

        String token = notificationService.getAndDeleteTokenByTicket("unknown-ticket");

        assertNull(token);
        verify(cache, never()).evict("unknown-ticket");
    }

    @Test
    @DisplayName("subscribe should return SseEmitter and register client connection")
    void subscribe_success() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(1L);
        doReturn(List.of(new SimpleGrantedAuthority(Permission.ADMIN_ACCESS.getValue())))
                .when(userDetails).getAuthorities();

        SseEmitter emitter = notificationService.subscribe(userDetails);

        assertNotNull(emitter);
    }

    @Test
    @DisplayName("sendToUser should publish NotificationEvent to Redis when Redis template is configured")
    void sendToUser_withRedis_publishesToChannel() throws JsonProcessingException {
        NotificationResponse response = new NotificationResponse(NotificationType.INFO, "Order shipped");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"recipientType\":\"USER\"}");

        notificationService.sendToUser(5L, response);

        verify(stringRedisTemplate).convertAndSend(eq(NotificationService.NOTIFICATION_CHANNEL), eq("{\"recipientType\":\"USER\"}"));
    }

    @Test
    @DisplayName("sendToUser should fall back to local delivery when Redis template is null")
    void sendToUser_withoutRedis_fallsBackToLocalDelivery() {
        ReflectionTestUtils.setField(notificationService, "stringRedisTemplate", null);

        NotificationResponse response = new NotificationResponse(NotificationType.INFO, "Order shipped");

        // Does not throw and does not call redis
        assertDoesNotThrow(() -> notificationService.sendToUser(5L, response));
    }

    @Test
    @DisplayName("sendToAdmins should publish NotificationEvent for ADMIN to Redis")
    void sendToAdmins_withRedis_publishesToChannel() throws JsonProcessingException {
        NotificationResponse response = new NotificationResponse(NotificationType.WARNING, "Product low in stock");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"recipientType\":\"ADMIN\"}");

        notificationService.sendToAdmins(response);

        verify(stringRedisTemplate).convertAndSend(eq(NotificationService.NOTIFICATION_CHANNEL), eq("{\"recipientType\":\"ADMIN\"}"));
    }

    @Test
    @DisplayName("onMessage should deserialize event and dispatch to user locally")
    void onMessage_userEvent_dispatchesLocally() throws Exception {
        NotificationEvent event = new NotificationEvent("USER", 42L, NotificationType.SUCCESS, "Hello");
        String json = "{\"recipientType\":\"USER\",\"recipientId\":42,\"type\":\"SUCCESS\",\"message\":\"Hello\"}";
        Message redisMessage = new DefaultMessage("ecom:sse:notifications".getBytes(StandardCharsets.UTF_8), json.getBytes(StandardCharsets.UTF_8));

        when(objectMapper.readValue(json, NotificationEvent.class)).thenReturn(event);

        assertDoesNotThrow(() -> notificationService.onMessage(redisMessage, null));
    }

    @Test
    @DisplayName("onMessage should deserialize event and dispatch to admin locally")
    void onMessage_adminEvent_dispatchesLocally() throws Exception {
        NotificationEvent event = new NotificationEvent("ADMIN", null, NotificationType.WARNING, "System alert");
        String json = "{\"recipientType\":\"ADMIN\",\"recipientId\":null,\"type\":\"WARNING\",\"message\":\"System alert\"}";
        Message redisMessage = new DefaultMessage("ecom:sse:notifications".getBytes(StandardCharsets.UTF_8), json.getBytes(StandardCharsets.UTF_8));

        when(objectMapper.readValue(json, NotificationEvent.class)).thenReturn(event);

        assertDoesNotThrow(() -> notificationService.onMessage(redisMessage, null));
    }

    @Test
    @DisplayName("onMessage should handle invalid message payload gracefully without throwing exception")
    void onMessage_invalidPayload_logsErrorGracefully() throws Exception {
        String invalidJson = "{corrupted json}";
        Message redisMessage = new DefaultMessage("ecom:sse:notifications".getBytes(StandardCharsets.UTF_8), invalidJson.getBytes(StandardCharsets.UTF_8));

        when(objectMapper.readValue(invalidJson, NotificationEvent.class)).thenThrow(new JsonProcessingException("Bad json") {});

        assertDoesNotThrow(() -> notificationService.onMessage(redisMessage, null));
    }
}
