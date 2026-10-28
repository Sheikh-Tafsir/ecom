const RedisConfig = require('../config/RedisConfig');
const {logger} = require('../config/logger');
const {USER_PRESENCE_EVENT, PRESENCE_QUERY_EVENT} = require('./socketEvents');
const {findContactUserIds} = require('../service/ChatService');
const {PRESENCE_PREFIX, LAST_SEEN_PREFIX, PRESENCE_TTL} = require('../constants/redis.constants');
const {userRoom} = require('../constants/room.constants');

/**
 * Track user coming online.
 * SADD presence:{userId} {socketId} + EXPIRE 120s
 * If SCARD === 1 (just came online), broadcast to all contacts
 */
const trackConnect = async (io, socket) => {
    const userId = socket.user.id;
    const socketId = socket.id;

    try {
        await RedisConfig.sadd(`${PRESENCE_PREFIX}${userId}`, socketId);
        await RedisConfig.expire(`${PRESENCE_PREFIX}${userId}`, PRESENCE_TTL);

        const count = await RedisConfig.scard(`${PRESENCE_PREFIX}${userId}`);
        if (count === 1) {
            // User just came online — broadcast to contacts
            const contactIds = await findContactUserIds(userId);
            for (const contactId of contactIds) {
                io.to(userRoom(contactId)).emit(USER_PRESENCE_EVENT, {
                    userId,
                    online: true,
                });
            }
        }
    } catch (err) {
        logger.error(`Presence trackConnect failed for ${userId}:`, err.message);
    }
};

/**
 * Track user going offline.
 * SREM presence:{userId} {socketId}
 * If SCARD === 0 (went fully offline), store lastSeen + broadcast offline
 */
const trackDisconnect = async (io, socket) => {
    const userId = socket.user.id;
    const socketId = socket.id;

    try {
        await RedisConfig.srem(`${PRESENCE_PREFIX}${userId}`, socketId);
        const count = await RedisConfig.scard(`${PRESENCE_PREFIX}${userId}`);

        if (count === 0) {
            const lastSeen = new Date().toISOString();
            await RedisConfig.set(`${LAST_SEEN_PREFIX}${userId}`, lastSeen);
            await RedisConfig.del(`${PRESENCE_PREFIX}${userId}`);

            const contactIds = await findContactUserIds(userId);
            for (const contactId of contactIds) {
                io.to(userRoom(contactId)).emit(USER_PRESENCE_EVENT, {
                    userId,
                    online: false,
                    lastSeen,
                });
            }
        }
    } catch (err) {
        logger.error(`Presence trackDisconnect failed for ${userId}:`, err.message);
    }
};

/**
 * Bulk query presence for multiple userIds.
 * Uses Redis pipeline for efficiency.
 * Returns { [userId]: { online: boolean, lastSeen: string|null } }
 */
const getPresence = async (userIds) => {
    if (!userIds || userIds.length === 0) return {};

    const pipeline = RedisConfig.pipeline();
    for (const uid of userIds) {
        pipeline.scard(`${PRESENCE_PREFIX}${uid}`);
        pipeline.get(`${LAST_SEEN_PREFIX}${uid}`);
    }

    const results = await pipeline.exec();
    const presenceMap = {};

    for (let i = 0; i < userIds.length; i++) {
        const countResult = results[i * 2];
        const lastSeenResult = results[i * 2 + 1];

        const count = countResult[1] || 0;
        const lastSeen = lastSeenResult[1] || null;

        presenceMap[userIds[i]] = {
            online: count > 0,
            lastSeen,
        };
    }

    return presenceMap;
};

/**
 * Register presence-query event handler on a socket.
 * Client sends an array of userIds, server responds with bulk presence.
 */
const setupPresenceHandler = (io, socket) => {
    socket.on(PRESENCE_QUERY_EVENT, async (data = {}, ack) => {
        try {
            const {userIds} = data;
            if (!Array.isArray(userIds) || userIds.length === 0) {
                return ack?.({});
            }

            // Cap at 500 to prevent abuse
            const capped = userIds.slice(0, 500);
            const presenceMap = await getPresence(capped);
            ack?.(presenceMap);
        } catch (err) {
            logger.error(`Presence query failed for ${socket.user.id}:`, err.message);
            ack?.({});
        }
    });
};

/**
 * Heartbeat: refresh TTL for all connected sockets' users.
 * Runs every 60s. Prevents zombie presence entries if EXPIRE was missed.
 */
const startPresenceHeartbeat = (io) => {
    const interval = setInterval(async () => {
        try {
            const sockets = await io.fetchSockets();
            const refreshedUsers = new Set();

            for (const s of sockets) {
                const userId = s.user?.id;
                if (userId && !refreshedUsers.has(userId)) {
                    refreshedUsers.add(userId);
                    await RedisConfig.expire(`${PRESENCE_PREFIX}${userId}`, PRESENCE_TTL);
                }
            }

            logger.info(`Presence heartbeat: refreshed TTL for ${refreshedUsers.size} users`);
        } catch (err) {
            logger.error('Presence heartbeat failed:', err.message);
        }
    }, 60_000);

    // Allow process to exit cleanly
    interval.unref();

    return interval;
};

module.exports = {
    trackConnect,
    trackDisconnect,
    getPresence,
    setupPresenceHandler,
    startPresenceHeartbeat,
};
