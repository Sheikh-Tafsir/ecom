package com.example.ecom.user.role.dto;

import com.example.ecom.common.enums.Permission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record RoleRequest(
        @NotBlank(message = "Role name is required")
        String name,

        @NotEmpty(message = "At least one permission is required")
        Set<Permission> permissions
) {
}
