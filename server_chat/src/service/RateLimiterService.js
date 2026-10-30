const CacheService = require('./CacheService');
const { RATE_LIMIT_PREFIX } = require('../constants/redis.constants');
const {
    MESSAGE_RATE_LIMIT,
    MESSAGE_RATE_WINDOW_SEC,
    FALLBACK_RATE_LIMIT,
    FALLBACK_RATE_WINDOW_MS,
} = require('../constants/chat.constants');
const { logger } = require('../config/logger');

// Process-local fallback limits
const FALLBACK_MAX_ENTRIES = 10000;

// Process-local fallback counter map (used when Redis/cache is temporarily offline)
const fallbackMemoryMap = new Map();

// Periodic cleanup: remove stale entries every 30 seconds to prevent memory leaks
const _cleanupInterval = setInterval(() => {
    const now = Date.now();
    for (const [key, entry] of fallbackMemoryMap) {
        if (now - entry.resetTime > FALLBACK_RATE_WINDOW_MS * 2) {
            fallbackMemoryMap.delete(key);
        }
    }
}, 30_000);
_cleanupInterval.unref();

/**
 * RateLimiterService — Manages rate limits across domain actions.
 *
 * Backed by CacheService (Redis) with automatic fallback to process-local
 * sliding window counters if the cache cluster is degraded or unreachable.
 */
class RateLimiterService {
    /**
     * Check if a user has exceeded the message sending rate limit.
     * @param {string} userId
     * @returns {Promise<boolean>} true if rate limited, false if allowed
     */
    async isMessageRateLimited(userId) {
        if (!userId) return false;

        const key = `${RATE_LIMIT_PREFIX}${userId}`;

        try {
            const count = await CacheService.incrementWithExpiry(key, MESSAGE_RATE_WINDOW_SEC);
            return count > MESSAGE_RATE_LIMIT;
        } catch (err) {
            logger.warn(`Cache rate-limiting unavailable for user ${userId}, using local fallback: ${err.message}`);
            return this._isMemoryLimited(userId);
        }
    }

    /**
     * In-memory sliding counter fallback per user.
     * @private
     */
    _isMemoryLimited(userId) {
        // Evict oldest entries if map exceeds max size
        if (fallbackMemoryMap.size >= FALLBACK_MAX_ENTRIES && !fallbackMemoryMap.has(userId)) {
            const firstKey = fallbackMemoryMap.keys().next().value;
            fallbackMemoryMap.delete(firstKey);
        }

        const now = Date.now();
        const entry = fallbackMemoryMap.get(userId);

        if (!entry || (now - entry.resetTime > FALLBACK_RATE_WINDOW_MS)) {
            fallbackMemoryMap.set(userId, { count: 1, resetTime: now });
            return false;
        }

        entry.count++;
        return entry.count > FALLBACK_RATE_LIMIT;
    }
}

module.exports = new RateLimiterService();
