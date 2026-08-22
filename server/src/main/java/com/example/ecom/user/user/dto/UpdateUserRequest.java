package com.example.ecom.user.user.dto;

import java.util.Set;

public record UpdateUserRequest(
        Set<String> roles,
        String status
) {
}
