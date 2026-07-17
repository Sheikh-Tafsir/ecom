package com.example.demo.role.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import com.example.demo.role.dto.RoleRequest;
import com.example.demo.role.dto.RoleResponse;
import com.example.demo.role.service.RoleService;
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
