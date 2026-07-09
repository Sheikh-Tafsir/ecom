export const APP_MODULE = Object.freeze({
    USER: "User",
    ORDER: "Order",
    SALE: "Sale",
});

export const GENDER_TYPE = Object.freeze({
    MALE: "Male",
    FEMALE: "Female",
});

export const USER_STATUS = Object.freeze({
    NOT_VERIFIED: "Not Verified",
    ACTIVE: "Active",
    SUSPENDED: 'Suspended',
    INACTIVE: "Inactive",
    DELETED: 'Deleted',
    BANNED: "Banned"
});

export const ROLE_PREFIX = "ROLE_";

export const USER_ROLE = Object.freeze({
    USER: 'ROLE_USER',
    ADMIN: 'ROLE_ADMIN',
    SUPER_ADMIN: 'ROLE_SUPER_ADMIN',
});

export const PERMISSION = Object.freeze({
    SUPER_ADMIN_ACCESS: "super_admin:access",
    ADMIN_ACCESS: "admin:access"
});

export const CONTENT_TYPE = Object.freeze({
    TEXT: "text",
    IMAGE: "image",
});

export const TOAST_TYPE = Object.freeze({
    INFO: "info",              // Passive info or message
    SUCCESS: "success",        // Completed successfully
    ERROR: "error",            // Something went wrong
    WARNING: "warning",        // Needs attention, but not fatal

    NEED_ACTION: "need action",         // Requires user decision/input
    BLOCKING: "blocking",               // Prevents user from continuing
    CONFIRMATION: "confirmation",       // Asks user to confirm or cancel
    PROCESSING: "processing",           // In-progress feedback
    CANCELLED: "cancelled",             // User or system cancelled something

    FIXED: "fixed"
})

export const CHAT_TYPE = Object.freeze({
    DIRECT: "direct",
    GROUP: 'group',
});

export const CHAT_MEMBER_TYPE = Object.freeze({
    ADMIN: "admin",
    MEMBER: 'member',
});

export const REGULAR_ACTION = Object.freeze({
    CREATE: "create",
    UPDATE: 'update',
    DELETE: 'delete',
});

export const PRODUCT_SORTBY = Object.freeze({
    NEWEST: {label: "Newest", value: "createdAt,DESC"},
    OLDEST: {label: "Oldest", value: "createdAt,ASC"},
    PRICE_LOW: {label: "Price: Low to High", value: "price,ASC"},
    PRICE_HIGH: {label: "Price: High to Low", value: "price,DESC"},
    NAME_ASC: {label: "Name: A to Z", value: "name,ASC"},
});

export const PAYMENT_METHOD = Object.freeze({
    CASH_ON_DELIVERY: "Cash on Delivery",
    BKASH: "Bkash"
});

export const ORDER_STATUS = Object.freeze({
    PENDING: "Pending",
    ACCEPTED: "Accepted",
    REJECTED: "Rejected",
    PROCESSING: "Processing",
    SHIPPED: "Shipped",
    DELIVERED: "Delivered",
    CANCELLED: "Cancelled",
});

export const ALERT_TYPE = {
    ADD: "Add",
    CONFIRM: "Confirm",
    EDIT: "Edit",
    DELETE: "Delete",
    DEFAULT: "Default",
};