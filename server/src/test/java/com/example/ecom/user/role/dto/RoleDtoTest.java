package com.example.ecom.user.role.dto;

import com.example.ecom.common.enums.Permission;
import com.example.ecom.common.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoleDtoTest {

    @Nested
    @DisplayName("RoleRequest Tests")
    class RoleRequestTest {

        @Test
        @DisplayName("Record constructor sets name and permissions")
        void constructor_validArgs_setsFields() {
            Set<Permission> permissions = Set.of(Permission.ADMIN_ACCESS, Permission.USER);
            RoleRequest request = new RoleRequest("ADMIN", permissions);

            assertEquals("ADMIN", request.name());
            assertEquals(2, request.permissions().size());
            assertTrue(request.permissions().contains(Permission.ADMIN_ACCESS));
        }

        @Test
        @DisplayName("Two equal records are equal")
        void equals_sameFields_returnsTrue() {
            Set<Permission> perms = Set.of(Permission.USER);
            RoleRequest r1 = new RoleRequest("USER", perms);
            RoleRequest r2 = new RoleRequest("USER", perms);

            assertEquals(r1, r2);
        }
    }

    @Nested
    @DisplayName("RoleResponse Tests")
    class RoleResponseTest {

        @Test
        @DisplayName("Construction from Role entity maps all fields")
        void constructor_fromEntity_setsAllFields() {
            UUID roleId = UUID.fromString("01927d74-097f-7afa-9ae5-748596071829");
            Role role = new Role();
            role.setId(roleId);
            role.setName("SUPER_ADMIN");
            role.setPermissions(Set.of(Permission.SUPER_ADMIN_ACCESS, Permission.ADMIN_ACCESS));

            RoleResponse response = new RoleResponse(role);

            assertEquals(roleId, response.getId());
            assertEquals("SUPER_ADMIN", response.getName());
            assertEquals(2, response.getPermissions().size());
            assertTrue(response.getPermissions().contains(Permission.SUPER_ADMIN_ACCESS));
        }

        @Test
        @DisplayName("No-args constructor creates empty response with empty permissions set")
        void noArgsConstructor_createsEmptyWithDefaultPermissions() {
            RoleResponse response = new RoleResponse();
            assertNull(response.getId());
            assertNull(response.getName());
            assertNotNull(response.getPermissions());
            assertTrue(response.getPermissions().isEmpty());
        }

        @Test
        @DisplayName("Setters update all fields")
        void setters_allFields_updateCorrectly() {
            RoleResponse response = new RoleResponse();
            UUID id = UUID.fromString("01927d74-1a8f-7bfa-8ae6-859607182930");

            response.setId(id);
            response.setName("DELIVERY_MAN");
            response.setPermissions(Set.of(Permission.DELIVERY_MAN_ACCESS));

            assertEquals(id, response.getId());
            assertEquals("DELIVERY_MAN", response.getName());
            assertTrue(response.getPermissions().contains(Permission.DELIVERY_MAN_ACCESS));
        }
    }
}
