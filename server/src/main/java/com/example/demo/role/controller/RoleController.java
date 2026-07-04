package com.example.demo.role.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.model.Role;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import com.example.demo.role.dto.RoleRequest;
import com.example.demo.role.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
public class RoleController {

    private final RoleService roleService;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Role>>> findAll() {
        List<Role> roles = roleService.findAll();
        return ResponseUtils.ok(roles, messageService.get("successfully.found", "Role List"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Role>> findById(@PathVariable Long id) {
        Role role = roleService.findById(id);
        return ResponseUtils.ok(role, messageService.get("successfully.found", "Role"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Role>> create(@Valid @RequestBody RoleRequest roleRequest) {
        Role role = roleService.create(roleRequest.name(), roleRequest.permissions());
        return ResponseUtils.ok(role, messageService.get("successfully.created", "Role"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Role>> update(@PathVariable Long id, @Valid @RequestBody RoleRequest roleRequest) {
        Role role = roleService.update(id, roleRequest.name(), roleRequest.permissions());
        return ResponseUtils.ok(role, messageService.get("successfully.updated", "Role"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseUtils.ok(messageService.get("successfully.deleted", "Role"));
    }
}
