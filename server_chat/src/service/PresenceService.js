const CacheService = require('./CacheService');
const { PRESENCE_PREFIX, LAST_SEEN_PREFIX, PRESENCE_TTL } = require('../constants/redis.constants');
const { logger } = require('../config/logger');

/**
 * PresenceService — Real-time User Presence & Availability Management
 *
 * Encapsulates presence tracking, multi-device socket management, and
 * last-seen status, shielding socket handlers from raw Redis data structures.
 */
class PresenceService {
    constructor() {
        this.client = CacheService.getUnderlyingClient();
    }

    /**
     * Track a socket connection for a user.
     * @param {string} userId - User ID
     * @param {string} socketId - Socket ID
     * @returns {Promise<{ isNewlyOnline: boolean, activeSocketCount: number }>}
     */
    async addConnection(userId, socketId) {
        if (!userId || !socketId) return { isNewlyOnline: false, activeSocketCount: 0 };

        try {
            const key = `${PRESENCE_PREFIX}${userId}`;
            await this.client.sadd(key, socketId);
            await this.client.expire(key, PRESENCE_TTL);

            const count = await this.client.scard(key);
            return {
                isNewlyOnline: count === 1,
                activeSocketCount: count,
            };
        } catch (err) {
            logger.error(`PresenceService.addConnection failed for ${userId}:`, err.message);
            return { isNewlyOnline: false, activeSocketCount: 0 };
        }
    }

    /**
     * Track a socket disconnect for a user.
     * @param {string} userId - User ID
     * @param {string} socketId - Socket ID
     * @returns {Promise<{ isNowOffline: boolean, lastSeen: string|null }>}
     */
    async removeConnection(userId, socketId) {
        if (!userId || !socketId) return { isNowOffline: false, lastSeen: null };

        try {
            const key = `${PRESENCE_PREFIX}${userId}`;
            await this.client.srem(key, socketId);
            const count = await this.client.scard(key);

            if (count === 0) {
                const lastSeen = new Date().toISOString();
                await this.client.set(`${LAST_SEEN_PREFIX}${userId}`, lastSeen);
                await this.client.del(key);
                return { isNowOffline: true, lastSeen };
            }

            return { isNowOffline: false, lastSeen: null };
        } catch (err) {
            logger.error(`PresenceService.removeConnection failed for ${userId}:`, err.message);
            return { isNowOffline: false, lastSeen: null };
        }
    }

    /**
     * Query presence and last-seen status in bulk using a pipeline.
     * @param {string[]} userIds - Array of user IDs
     * @returns {Promise<Record<string, { online: boolean, lastSeen: string|null }>>}
     */
    async getUsersPresence(userIds = []) {
        if (!Array.isArray(userIds) || userIds.length === 0) return {};

        try {
            const pipeline = this.client.pipeline();
            for (const uid of userIds) {
                pipeline.scard(`${PRESENCE_PREFIX}${uid}`);
                pipeline.get(`${LAST_SEEN_PREFIX}${uid}`);
            }

            const results = await pipeline.exec();
            const presenceMap = {};

            for (let i = 0; i < userIds.length; i++) {
                const countResult = results[i * 2];
                const lastSeenResult = results[i * 2 + 1];

                const count = (countResult && countResult[1]) || 0;
                const lastSeen = (lastSeenResult && lastSeenResult[1]) || null;

                presenceMap[userIds[i]] = {
                    online: count > 0,
                    lastSeen,
                };
            }

            return presenceMap;
        } catch (err) {
            logger.error('PresenceService.getUsersPresence failed:', err.message);
            return {};
        }
    }

    /**
     * Refresh TTL for an array of active user IDs.
     * @param {string[]} userIds
     * @returns {Promise<number>} Number of refreshed keys
     */
    async refreshHeartbeat(userIds = []) {
        if (!Array.isArray(userIds) || userIds.length === 0) return 0;

        try {
            const pipeline = this.client.pipeline();
            for (const userId of userIds) {
                pipeline.expire(`${PRESENCE_PREFIX}${userId}`, PRESENCE_TTL);
            }
            await pipeline.exec();
            return userIds.length;
        } catch (err) {
            logger.error('PresenceService.refreshHeartbeat failed:', err.message);
            return 0;
        }
    }
}

module.exports = new PresenceService();
