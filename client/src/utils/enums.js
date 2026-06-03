export const GENDER_TYPE = Object.freeze({
    MALE: "Male",
    FEMALE: "Female",
});

export const PARTICIPANT_TYPE = Object.freeze({
  ASD: "ASD",
  TD: "TD"
});

export const USER_STATUS = Object.freeze({
    INACTIVE: "Inactive",
    ACTIVE: "Active",
    SUSPENDED: 'Suspended',
    DELETED: 'Deleted'
});

export const USER_ROLE = Object.freeze({
  USER: 'ROLE_USER',
  ADMIN: 'ROLE_ADMIN',
  SUPER_ADMIN: 'ROLE_SUPER_ADMIN',
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


export const DAYS_OF_WEEK = Object.freeze({
    SUNDAY: "1",
    MONDAY: "2",
    TUESDAY: "3",
    WEDNESDAY: "4",
    THURSDAY: "5",
    FRIDAY: "6",
    SATURDAY: "7",
});

export const TRANSACTION_TYPE = Object.freeze({
    EXPENSE: "Expense",
    INCOME: "Income",
    // TRANSFER: "Transfer",
});

export const MONTHS = [
  { value: "1", label: "January" },
  { value: "2", label: "February" },
  { value: "3", label: "March" },
  { value: "4", label: "April" },
  { value: "5", label: "May" },
  { value: "6", label: "June" },
  { value: "7", label: "July" },
  { value: "8", label: "August" },
  { value: "9", label: "September" },
  { value: "10", label: "October" },
  { value: "11", label: "November" },
  { value: "12", label: "December" },
]

export const PRIORITY_LEVEL = Object.freeze({
  Low: "1",
  Medium: "2",
  High: "3",
});

export const getPriorityKey = (value) =>{
  return Object.keys(PRIORITY_LEVEL).find(
    (key) => PRIORITY_LEVEL[key] == value
  );
}

export const COURSES = Object.freeze({
  PHYSICS: "Physics",
  CHEMISTRY: "Chemistry",
  MATH: "Math"
})

export const DIFFICULTY_LEVEL = Object.freeze({
  EASY: "Easy",
  Medium: "Medium",
  High: "High",
});

export const QUESTION_OPTION_MAP = ["a", "b", "c", "d"];

export const PRODUCT_SORTBY = Object.freeze({
  DATE_DESC: { label: "Newest First", value: "createdAt,DESC" },
  PRICE_ASC: { label: "Price: Low to High", value: "price,ASC" },
  PRICE_DESC: { label: "Price: High to Low", value: "price,DESC" },
  NAME_ASC: { label: "Name: A to Z", value: "name,ASC" },
});

export const PAYMENT_METHOD = Object.freeze({
  CASH_ON_DELIVERY: "Cash on Delivery",
  CARD: "Card",
});

export const ORDER_STATUS = Object.freeze({
  PENDING: "Pending",
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