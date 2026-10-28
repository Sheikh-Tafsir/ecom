// Redis key prefixes
const USER_CACHE_PREFIX = 'cache:user:';
const PRESENCE_PREFIX = 'presence:';
const LAST_SEEN_PREFIX = 'presence:lastseen:';
const RATE_LIMIT_PREFIX = 'ratelimit:msg:';

// Redis TTLs (seconds)
const USER_CACHE_TTL = 600;       // 10 minutes
const PRESENCE_TTL = 120;         // 2 minutes

// Redis Pub/Sub channels (must match Java Main Server constants)
const NOTIFICATION_CHANNEL = 'ecom:notifications';
const USER_UPDATES_CHANNEL = 'ecom:user-updates';

module.exports = {
    USER_CACHE_PREFIX,
    PRESENCE_PREFIX,
    LAST_SEEN_PREFIX,
    RATE_LIMIT_PREFIX,
    USER_CACHE_TTL,
    PRESENCE_TTL,
    NOTIFICATION_CHANNEL,
    USER_UPDATES_CHANNEL,
};
