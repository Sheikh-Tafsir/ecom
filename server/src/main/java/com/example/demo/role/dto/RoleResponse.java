package com.example.demo.role.dto;

import com.example.demo.common.enums.Permission;
import com.example.demo.common.model.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {

    private Long id;

    private String name;

    private Set<Permission> permissions = new HashSet<>();

    public RoleResponse(Role role) {
        id = role.getId();
        name = role.getName();
        permissions = role.getPermissions();
    }
}
