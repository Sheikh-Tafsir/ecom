const CONTENT_TYPE = Object.freeze({
    TEXT: "text",
    IMAGE: "image",
});

const CHAT_TYPE = Object.freeze({
    DIRECT: "direct",
    GROUP: 'group',
});

const CHAT_MEMBER_TYPE = Object.freeze({
    ADMIN: "admin",
    MEMBER: 'member',
});

const UserStatus = Object.freeze({
    ACTIVE: 'active',
    INACTIVE: 'inactive',
    DELETED: 'deleted',
});

const UserRole = Object.freeze({
    ADMIN: 'admin',
    SUPER_ADMIN: 'super_admin',
    USER: 'user',
});

const Permission = Object.freeze({
    SUPER_ADMIN_ACCESS: "super_admin:access",
    ADMIN_ACCESS: "admin:access"
});

module.exports = {
    CONTENT_TYPE,
    CHAT_TYPE,
    CHAT_MEMBER_TYPE,
    UserStatus,
    UserRole,
    Permission,
}