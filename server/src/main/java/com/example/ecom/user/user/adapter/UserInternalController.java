package com.example.ecom.user.user.adapter;

import com.example.ecom.common.client.user.UserBasicDto;
import com.example.ecom.common.client.user.UserClient;
import com.example.ecom.common.dto.ApiResponse;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.common.utils.ResponseUtils;
import com.example.ecom.user.user.dto.UserSearchResponse;
import com.example.ecom.user.user.model.User;
import com.example.ecom.user.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Internal REST endpoint for cross-service user operations.
 * Secured by {@code X-Internal-Service-Key} at the Spring Security layer.
 * In monolith mode, these endpoints exist but are unused
 * (callers use {@link UserClientAdapter} directly).
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class UserInternalController implements UserClient {

    private final UserService userService;
    private final MessageService messageService;

    @GetMapping("/{id}")
    @Override
    public UserBasicDto findUserById(@PathVariable UUID id) {
        User user = userService.findByIdHelper(id);
        return new UserBasicDto(user.getId(), user.getName(), user.getImage());
    }

    /**
     * Batch user lookup used by Chat Server for M2M communication.
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<UserSearchResponse>>> findAllByIds(
            @Valid @NotEmpty(message = "User IDs list cannot be empty")
            @Size(max = 100, message = "Maximum of 100 user IDs allowed per batch request")
            @RequestBody List<UUID> ids) {

        List<UserSearchResponse> users = userService.findAllByIds(ids);
        return ResponseUtils.ok(users, messageService.get("successfully.found", "Users"));
    }
}
