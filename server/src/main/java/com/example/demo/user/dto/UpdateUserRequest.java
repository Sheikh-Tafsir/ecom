package com.example.demo.user.dto;

import java.util.Set;

public record UpdateUserRequest(
        Set<String> roles,
        String status
) {
}
