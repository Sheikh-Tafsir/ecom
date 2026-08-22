package com.example.ecom.user.role.dto;

import com.example.ecom.common.enums.Permission;
import com.example.ecom.common.model.Role;
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<Permission> getPermissions() { return permissions; }
    public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }
}
