export const APP_MODULE = Object.freeze({
    USER: "User",
    ORDER: "Order",
    SALE: "Sale",
});

export const REGULAR_ACTION = Object.freeze({
    CREATE: "create",
    UPDATE: 'update',
    DELETE: 'delete',
});

export const TOAST_TYPE = Object.freeze({
    INFO: "Info",              // Passive info or message
    SUCCESS: "Success",        // Completed successfully
    ERROR: "Error",            // Something went wrong
    WARNING: "Warning",        // Needs attention, but not fatal

    NEED_ACTION: "Need action",         // Requires user decision/input
    BLOCKING: "Blocking",               // Prevents user from continuing
    CONFIRMATION: "Confirmation",       // Asks user to confirm or cancel
    PROCESSING: "Processing",           // In-progress feedback
    CANCELLED: "Cancelled",             // User or system cancelled something

    FIXED: "Fixed"
})

export const ALERT_TYPE = {
    ADD: "Add",
    CONFIRM: "Confirm",
    EDIT: "Edit",
    DELETE: "Delete",
    DEFAULT: "Default",
};
