-- insert roles
INSERT INTO roles (id, name) VALUES (1, 'ROLE_SUPER_ADMIN');
INSERT INTO roles (id, name) VALUES (2, 'ROLE_ADMIN');
INSERT INTO roles (id, name) VALUES (3, 'ROLE_USER');

-- insert Super Admin
INSERT INTO users (id, name, email, gender, status)
    VALUES (1, 'Tafsir Rahman', '190041130tafsir@gmail.com', 'MALE', 'ACTIVE');

INSERT INTO users_roles (user_id, role_id) VALUES (1, 1);