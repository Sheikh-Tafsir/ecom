package com.example.ecom.user.role.controller;

import com.example.ecom.common.dto.ApiResponse;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.common.utils.ResponseUtils;
import com.example.ecom.user.role.dto.RoleRequest;
import com.example.ecom.user.role.dto.RoleResponse;
import com.example.ecom.user.role.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    private final MessageService messageService;

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<com.example.ecom.common.enums.Permission[]>> getPermissions() {
        return ResponseUtils.ok(com.example.ecom.common.enums.Permission.values(), messageService.get("successfully.found", "Permissions"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> findAll() {
        List<RoleResponse> roles = roleService.findAll();
        return ResponseUtils.ok(roles, messageService.get("successfully.found", "Role List"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> findById(@PathVariable Long id) {
        RoleResponse role = roleService.findById(id);
        return ResponseUtils.ok(role, messageService.get("successfully.found", "Role"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(@Valid @RequestBody RoleRequest roleRequest) {
        Long roleId = roleService.create(roleRequest.name(), roleRequest.permissions());
        return ResponseUtils.ok(roleId, messageService.get("successfully.created", "Role"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> update(@PathVariable Long id, @Valid @RequestBody RoleRequest roleRequest) {
        RoleResponse role = roleService.update(id, roleRequest.name(), roleRequest.permissions());
        return ResponseUtils.ok(role, messageService.get("successfully.updated", "Role"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseUtils.ok(messageService.get("successfully.deleted", "Role"));
    }
}
