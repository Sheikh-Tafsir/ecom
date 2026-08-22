-- ==========================================================
-- E-Commerce Flyway Database Migration: Initial Seed Data
-- Version: V3__initial_data.sql
-- ==========================================================

-- 1. Seed Core Roles
INSERT INTO roles (id, name) VALUES
(1, 'Super Admin'),
(2, 'Admin'),
(3, 'Delivery Main'),
(4, 'User')
ON CONFLICT (name) DO NOTHING;

-- 2. Seed Role Permissions
-- Super Admin gets all permissions
INSERT INTO role_permissions (role_id, permission) VALUES
(1, 'SUPER_ADMIN_ACCESS'),
(1, 'ADMIN_ACCESS'),
(1, 'DELIVERY_MAN_ACCESS'),
-- Admin gets ADMIN_ACCESS
(2, 'ADMIN_ACCESS'),
-- Delivery man gets DELIVERY_MAN_ACCESS
(3, 'DELIVERY_MAN_ACCESS')
ON CONFLICT (role_id, permission) DO NOTHING;

-- 3. Adjust sequence if needed
SELECT setval('roles_id_seq', (SELECT COALESCE(MAX(id), 1) FROM roles));
