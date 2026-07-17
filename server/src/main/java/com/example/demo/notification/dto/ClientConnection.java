package com.example.demo.notification.dto;

import com.example.demo.common.enums.Permission;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;

public record ClientConnection(
        SseEmitter emitter,
        Set<Permission> permissions
) {
}
