package com.example.ecom.notification.controller;

import com.example.ecom.common.dto.ApiResponse;
import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.utils.ResponseUtils;
import com.example.ecom.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/sse-token")
    public ResponseEntity<ApiResponse<String>> getSseTicket(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String ticket = notificationService.generateSseAuthToken(userDetails);
        return ResponseUtils.ok(ticket, "SSE ticket generated");
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return notificationService.subscribe(userDetails);
    }
}
