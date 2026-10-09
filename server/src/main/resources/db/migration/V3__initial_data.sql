-- ==========================================================
-- E-Commerce Flyway Database Migration: Seed Data
-- Version: V3__initial_data.sql
-- ==========================================================

-- 1. Seed Core Roles (UUIDs are stable constants for well-known roles)
INSERT INTO roles (id, name) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Super Admin'),
    ('00000000-0000-0000-0000-000000000002', 'Admin'),
    ('00000000-0000-0000-0000-000000000003', 'Delivery Man'),
    ('00000000-0000-0000-0000-000000000004', 'User')
ON CONFLICT (name) DO NOTHING;

-- 2. Seed Role Permissions
INSERT INTO role_permissions (role_id, permission) VALUES
    -- Super Admin gets all permissions
    ('00000000-0000-0000-0000-000000000001', 'SUPER_ADMIN_ACCESS'),
    ('00000000-0000-0000-0000-000000000001', 'ADMIN_ACCESS'),
    ('00000000-0000-0000-0000-000000000001', 'DELIVERY_MAN_ACCESS'),
    -- Admin
    ('00000000-0000-0000-0000-000000000002', 'ADMIN_ACCESS'),
    -- Delivery Man
    ('00000000-0000-0000-0000-000000000003', 'DELIVERY_MAN_ACCESS')
ON CONFLICT (role_id, permission) DO NOTHING;

-- 3. Seed Super Admin user (fixed UUID — stable across all deployments)
INSERT INTO users (id, name, email, password, status, created_at, updated_at) VALUES
    (
        '00000000-0000-7000-8000-000000000001',
        'Tafsir Rahman',
        'tafsirrahman26@gmail.com',
        '$2a$10$23cmrFd71X2.HqvsbIPSZuUXBrxa6mZchbNBgW/3qgkk8c6kkHhyy',
        'ACTIVE',
        now(),
        now()
    )
ON CONFLICT (email) DO NOTHING;

-- 4. Assign Super Admin role to the seeded admin user
INSERT INTO user_roles (user_id, role_id)
VALUES (
    '00000000-0000-7000-8000-000000000001',
    '00000000-0000-0000-0000-000000000001'
)
ON CONFLICT DO NOTHING;
