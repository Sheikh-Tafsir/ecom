// Pagination defaults
const CHAT_LIST_DEFAULT_SIZE = 15;
const CHAT_MESSAGE_DEFAULT_SIZE = 15;

// Message constraints
const MAX_MESSAGE_LENGTH = 4096;

// Rate limiting — Redis-backed sliding window (per user across all sockets)
const MESSAGE_RATE_LIMIT = 20;
const MESSAGE_RATE_WINDOW_SEC = 10;

// Rate limiting — in-memory fallback (per socket, when Redis is unavailable)
const FALLBACK_RATE_LIMIT = 5;
const FALLBACK_RATE_WINDOW_MS = 1000;

// Cloudinary upload
const CLOUDINARY_CHAT_FOLDER = 'chat-images';
const UPLOAD_FIELD_NAME = 'image';

module.exports = {
    CHAT_LIST_DEFAULT_SIZE,
    CHAT_MESSAGE_DEFAULT_SIZE,
    MAX_MESSAGE_LENGTH,
    MESSAGE_RATE_LIMIT,
    MESSAGE_RATE_WINDOW_SEC,
    FALLBACK_RATE_LIMIT,
    FALLBACK_RATE_WINDOW_MS,
    CLOUDINARY_CHAT_FOLDER,
    UPLOAD_FIELD_NAME,
};
