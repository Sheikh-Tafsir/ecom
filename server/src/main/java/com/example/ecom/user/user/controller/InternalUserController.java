package com.example.ecom.user.user.controller;

import com.example.ecom.common.dto.ApiResponse;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.common.utils.ResponseUtils;
import com.example.ecom.user.user.dto.UserSearchResponse;
import com.example.ecom.user.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;
    private final MessageService messageService;

    @Value("${app.internal.service-key}")
    private String internalServiceKey;

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<UserSearchResponse>>> findAllByIds(
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String providedKey,
            @Valid @NotEmpty(message = "User IDs list cannot be empty")
            @Size(max = 100, message = "Maximum of 100 user IDs allowed per batch request")
            @RequestBody List<UUID> ids) {

        if (providedKey == null || !providedKey.equals(internalServiceKey)) {
            log.warn("Unauthorized internal service request: invalid or missing X-Internal-Service-Key");
            throw new AccessDeniedException("Invalid or missing internal service key");
        }

        List<UserSearchResponse> users = userService.findAllByIds(ids);
        return ResponseUtils.ok(users, messageService.get("successfully.found", "Users"));
    }
}
