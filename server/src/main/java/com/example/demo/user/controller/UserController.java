package com.example.demo.user.controller;

import com.example.demo.common.enums.UserStatus;
import com.example.demo.user.service.UserService;
import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.model.User;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.example.demo.common.enums.UserStatus.BANNED;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<User>>> findAll(Pageable pageable,
                                                           @RequestParam(required = false) String name,
                                                           @RequestParam(required = false) String role,
                                                           @RequestParam(required = false) String status) {

        Page<User> users = userService.findAll(pageable, name, role, status);
        return ResponseUtils.ok(users, messageService.get("successfully.found", "User List"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> findById(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseUtils.ok(user, messageService.get("successfully.found", "User"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.banned(id);
        return ResponseUtils.ok(messageService.get("successfully.deleted", "User"));
    }
}
