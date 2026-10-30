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
     * Uses a Lua script to guarantee INCR + EXPIRE happen in a single round-trip.
     * @param {string} key
     * @param {number} windowSeconds
     * @returns {Promise<number>}
     */
    async incrementWithExpiry(key, windowSeconds) {
        try {
            const result = await RedisConfig.eval(
                `local c = redis.call('INCR', KEYS[1])
             if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
             return c`,
                1, key, windowSeconds
            );
            return result;
        } catch (err) {
            logger.warn(`Cache increment failed for key "${key}":`, err.message);
            throw err;
        }
    }

    /**
     * Add one or more members to a Redis set.
     * @param {string} key
     * @param {...string} members
     * @returns {Promise<number>} Number of new members added
     */
    async sadd(key, ...members) {
        try {
            return await RedisConfig.sadd(key, ...members);
        } catch (err) {
            logger.warn(`Cache sadd failed for key "${key}":`, err.message);
            return 0;
        }
    }

    /**
     * Remove one or more members from a Redis set.
     * @param {string} key
     * @param {...string} members
     * @returns {Promise<number>} Number of members removed
     */
    async srem(key, ...members) {
        try {
            return await RedisConfig.srem(key, ...members);
        } catch (err) {
            logger.warn(`Cache srem failed for key "${key}":`, err.message);
            return 0;
        }
    }

    /**
     * Get the number of members in a Redis set.
     * @param {string} key
     * @returns {Promise<number>}
     */
    async scard(key) {
        try {
            return await RedisConfig.scard(key);
        } catch (err) {
            logger.warn(`Cache scard failed for key "${key}":`, err.message);
            return 0;
        }
    }

    /**
     * Set a key's TTL in seconds.
     * @param {string} key
     * @param {number} seconds
     * @returns {Promise<number>} 1 if set, 0 if key doesn't exist
     */
    async expire(key, seconds) {
        try {
            return await RedisConfig.expire(key, seconds);
        } catch (err) {
            logger.warn(`Cache expire failed for key "${key}":`, err.message);
            return 0;
        }
    }

    /**
     * Create a pipelined batch of commands for atomic execution.
     * Callers chain Redis commands on the returned pipeline, then call .exec().
     * @returns {import('ioredis').Pipeline}
     */
    pipeline() {
        return RedisConfig.pipeline();
    }

    /**
     * Check if the underlying cache connection is healthy.
     * @returns {boolean}
     */
    isHealthy() {
        return RedisConfig.status === 'ready' || RedisConfig.status === 'connect';
    }

    /**
     * Direct access to underlying Redis client for infrastructure-layer components
     * that require operations beyond this abstraction (Pub/Sub duplicate connections,
     * Socket.IO adapter wiring). Domain-layer code should use CacheService methods instead.
     * @returns {import('ioredis').Redis}
     */
    getUnderlyingClient() {
        return RedisConfig;
    }
}

module.exports = new CacheService();
