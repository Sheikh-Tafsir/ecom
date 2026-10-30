const RedisConfig = require('../config/RedisConfig');
const { logger } = require('../config/logger');

/**
 * CacheService — Abstract Key-Value Cache Layer
 *
 * Wraps cache operations with error handling, logging, and unified semantics,
 * shielding services, handlers, and middlewares from direct Redis coupling.
 */
class CacheService {
    /**
     * Retrieve a value by key. Returns null on miss or on cache error (fail-open).
     * @param {string} key
     * @returns {Promise<string|null>}
     */
    async get(key) {
        try {
            return await RedisConfig.get(key);
        } catch (err) {
            logger.warn(`Cache get failed for key "${key}":`, err.message);
            return null;
        }
    }

    /**
     * Retrieve a value by key. Throws if the cache client encounters an error (fail-closed).
     * @param {string} key
     * @returns {Promise<string|null>}
     */
    async getOrThrow(key) {
        return await RedisConfig.get(key);
    }

    /**
     * Store a key-value pair with an optional TTL in seconds or Redis option flags.
     * Supports:
     *   - set(key, value)
     *   - set(key, value, ttlSeconds)
     *   - set(key, value, 'EX', ttlSeconds)
     * @param {string} key
     * @param {string} value
     * @param {...*} args
     * @returns {Promise<string|null>}
     */
    async set(key, value, ...args) {
        try {
            if (args.length === 1 && typeof args[0] === 'number') {
                return await RedisConfig.set(key, value, 'EX', args[0]);
            }
            if (args.length > 0) {
                return await RedisConfig.set(key, value, ...args);
            }
            return await RedisConfig.set(key, value);
        } catch (err) {
            logger.warn(`Cache set failed for key "${key}":`, err.message);
            return null;
        }
    }

    /**
     * Delete one or more keys.
     * @param {string|string[]} keys
     * @returns {Promise<number>}
     */
    async del(keys) {
        try {
            if (Array.isArray(keys)) {
                return await RedisConfig.del(...keys);
            }
            return await RedisConfig.del(keys);
        } catch (err) {
            logger.warn(`Cache del failed for key(s) "${keys}":`, err.message);
            return 0;
        }
    }

    /**
     * Atomically increment a counter key and set an expiration window on initial creation.
     * Used for rate limiting and sliding counters.
     * @param {string} key
     * @param {number} windowSeconds
     * @returns {Promise<number>}
     */
    async incrementWithExpiry(key, windowSeconds) {
        try {
            const count = await RedisConfig.incr(key);
            if (count === 1) {
                await RedisConfig.expire(key, windowSeconds);
            }
            return count;
        } catch (err) {
            logger.warn(`Cache increment failed for key "${key}":`, err.message);
            throw err;
        }
    }

    /**
     * Check if the underlying cache connection is healthy.
     * @returns {boolean}
     */
    isHealthy() {
        return RedisConfig.status === 'ready' || RedisConfig.status === 'connect';
    }

    /**
     * Direct access to underlying client for infrastructure components (Pub/Sub duplicate).
     * @returns {import('ioredis').Redis}
     */
    getUnderlyingClient() {
        return RedisConfig;
    }
}

module.exports = new CacheService();
