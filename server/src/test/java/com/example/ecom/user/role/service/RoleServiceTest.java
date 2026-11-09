package com.example.ecom.user.role.service;

import com.example.ecom.common.enums.Permission;
import com.example.ecom.common.model.Role;
import com.example.ecom.user.role.dto.RoleResponse;
import com.example.ecom.user.role.repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    private UUID roleId;
    private Role role;
    private Set<Permission> permissions;

    @BeforeEach
    void setUp() {
        roleId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");

        permissions = new HashSet<>();
        permissions.add(Permission.ADMIN_ACCESS);
        permissions.add(Permission.USER);

        role = new Role();
        role.setId(roleId);
        role.setName("Admin");
        role.setPermissions(permissions);
    }

    // ── findAll ─────────────────────────────────────────────────

    @Test
    @DisplayName("findAll should return list of role responses")
    void findAll_rolesExist_returnsRoleResponseList() {
        Role role2 = new Role();
        role2.setId(UUID.randomUUID());
        role2.setName("User");
        role2.setPermissions(Set.of(Permission.USER));

        when(roleRepository.findAll()).thenReturn(List.of(role, role2));

        List<RoleResponse> result = roleService.findAll();

        assertEquals(2, result.size());
        assertEquals("Admin", result.get(0).getName());
        assertEquals("User", result.get(1).getName());
        verify(roleRepository).findAll();
    }

    @Test
    @DisplayName("findAll should return empty list when no roles exist")
    void findAll_noRoles_returnsEmptyList() {
        when(roleRepository.findAll()).thenReturn(Collections.emptyList());

        List<RoleResponse> result = roleService.findAll();

        assertTrue(result.isEmpty());
        verify(roleRepository).findAll();
    }

    // ── findById ────────────────────────────────────────────────

    @Test
    @DisplayName("findById should return role response when role exists")
    void findById_existingRole_returnsRoleResponse() {
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        RoleResponse result = roleService.findById(roleId);

        assertNotNull(result);
        assertEquals(roleId, result.getId());
        assertEquals("Admin", result.getName());
        assertTrue(result.getPermissions().contains(Permission.ADMIN_ACCESS));
        verify(roleRepository).findById(roleId);
    }

    @Test
    @DisplayName("findById should throw EntityNotFoundException when role not found")
    void findById_nonExistentRole_throwsEntityNotFoundException() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        when(roleRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> roleService.findById(nonExistentId));

        verify(roleRepository).findById(nonExistentId);
    }

    // ── create ──────────────────────────────────────────────────

    @Test
    @DisplayName("create should persist role and return id when name is unique")
    void create_uniqueName_savesRoleAndReturnsId() {
        when(roleRepository.findByName("Admin")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role saved = invocation.getArgument(0);
            saved.setId(roleId);
            return saved;
        });

        UUID result = roleService.create("Admin", permissions);

        assertNotNull(result);
        verify(roleRepository).findByName("Admin");
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    @DisplayName("create should throw ValidationException when name already exists")
    void create_duplicateName_throwsValidationException() {
        when(roleRepository.findByName("Admin")).thenReturn(Optional.of(role));

        assertThrows(ValidationException.class, () -> roleService.create("Admin", permissions));

        verify(roleRepository).findByName("Admin");
        verify(roleRepository, never()).save(any());
    }

    // ── update ──────────────────────────────────────────────────

    @Test
    @DisplayName("update should modify existing role and return response")
    void update_existingRoleUniqueName_updatesAndReturnsResponse() {
        Set<Permission> newPermissions = Set.of(Permission.SUPER_ADMIN_ACCESS);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(roleRepository.findByName("Super Admin")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        RoleResponse result = roleService.update(roleId, "Super Admin", newPermissions);

        assertNotNull(result);
        verify(roleRepository).findById(roleId);
        verify(roleRepository).findByName("Super Admin");
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    @DisplayName("update should allow keeping the same name for the same role")
    void update_sameName_updatesWithoutConflict() {
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(roleRepository.findByName("Admin")).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        RoleResponse result = roleService.update(roleId, "Admin", permissions);

        assertNotNull(result);
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    @DisplayName("update should throw ValidationException when name conflicts with another role")
    void update_nameConflictsWithAnotherRole_throwsValidationException() {
        Role otherRole = new Role();
        otherRole.setId(UUID.fromString("00000000-0000-0000-0000-000000000011"));
        otherRole.setName("Admin");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(roleRepository.findByName("Admin")).thenReturn(Optional.of(otherRole));

        assertThrows(ValidationException.class, () -> roleService.update(roleId, "Admin", permissions));

        verify(roleRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should throw EntityNotFoundException when role not found")
    void update_nonExistentRole_throwsEntityNotFoundException() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000012");
        when(roleRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> roleService.update(nonExistentId, "Admin", permissions));

        verify(roleRepository).findById(nonExistentId);
        verify(roleRepository, never()).save(any());
    }

    // ── delete ──────────────────────────────────────────────────

    @Test
    @DisplayName("delete should remove existing role")
    void delete_existingRole_deletesRole() {
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        roleService.delete(roleId);

        verify(roleRepository).findById(roleId);
        verify(roleRepository).delete(role);
    }

    @Test
    @DisplayName("delete should throw EntityNotFoundException when role not found")
    void delete_nonExistentRole_throwsEntityNotFoundException() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000013");
        when(roleRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> roleService.delete(nonExistentId));

        verify(roleRepository).findById(nonExistentId);
        verify(roleRepository, never()).delete(any());
    }

    // ── findByName ──────────────────────────────────────────────

    @Test
    @DisplayName("findByName should return role when name exists")
    void findByName_existingName_returnsRole() {
        when(roleRepository.findByName("Admin")).thenReturn(Optional.of(role));

        Role result = roleService.findByName("Admin");

        assertNotNull(result);
        assertEquals("Admin", result.getName());
        verify(roleRepository).findByName("Admin");
    }

    @Test
    @DisplayName("findByName should return null when name does not exist")
    void findByName_nonExistentName_returnsNull() {
        when(roleRepository.findByName("NonExistent")).thenReturn(Optional.empty());

        Role result = roleService.findByName("NonExistent");

        assertNull(result);
        verify(roleRepository).findByName("NonExistent");
    }

    @Test
    @DisplayName("findByName should return null when name is null")
    void findByName_nullName_returnsNull() {
        Role result = roleService.findByName(null);

        assertNull(result);
        verify(roleRepository, never()).findByName(any());
    }

    @Test
    @DisplayName("findByName should return null when name is empty")
    void findByName_emptyName_returnsNull() {
        Role result = roleService.findByName("");

        assertNull(result);
        verify(roleRepository, never()).findByName(any());
    }

    @Test
    @DisplayName("findByName should return null when name is blank")
    void findByName_blankName_returnsNull() {
        Role result = roleService.findByName("   ");

        assertNull(result);
        verify(roleRepository, never()).findByName(any());
    }

    // ── findByIdHelper ──────────────────────────────────────────

    @Test
    @DisplayName("findByIdHelper should return role entity when found")
    void findByIdHelper_existingId_returnsRole() {
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        Role result = roleService.findByIdHelper(roleId);

        assertNotNull(result);
        assertEquals(roleId, result.getId());
        verify(roleRepository).findById(roleId);
    }

    @Test
    @DisplayName("findByIdHelper should throw EntityNotFoundException when not found")
    void findByIdHelper_nonExistentId_throwsEntityNotFoundException() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000014");
        when(roleRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> roleService.findByIdHelper(nonExistentId));

        assertTrue(exception.getMessage().contains(nonExistentId.toString()));
        verify(roleRepository).findById(nonExistentId);
    }
}
