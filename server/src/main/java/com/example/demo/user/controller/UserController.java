package com.example.demo.user.controller;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.user.dto.UpdateUserRequest;
import com.example.demo.user.dto.UserResponse;
import com.example.demo.user.dto.UserSearchResponse;
import com.example.demo.user.service.UserService;
import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> findAll(Pageable pageable,
                                                           @RequestParam(required = false) String name,
                                                           @RequestParam(required = false) String role,
                                                           @RequestParam(required = false) String status) {

        log.debug("role: {}, status: {}", role, status);
        Page<UserResponse> users = userService.findAll(pageable, name, role, status);
        return ResponseUtils.ok(users, messageService.get("successfully.found", "User List"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserSearchResponse>>> findForNameSearch(@RequestParam(required = false) String name,
                                                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<UserSearchResponse> users = userService.findAllByName(name, userDetails);
        return ResponseUtils.ok(users, messageService.get("successfully.found", "User search List"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> findById(@PathVariable Long id) {
        UserResponse user = userService.findById(id);
        return ResponseUtils.ok(user, messageService.get("successfully.found", "User"));
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateUserRequest request) {

        UserResponse user = userService.update(id, request);
        return ResponseUtils.ok(user, messageService.get("successfully.updated", "User"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.banned(id);
        return ResponseUtils.ok(messageService.get("successfully.deleted", "User"));
    }
}
