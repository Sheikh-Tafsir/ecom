const PresenceService = require('../service/PresenceService');
const { USER_PRESENCE_EVENT, PRESENCE_QUERY_EVENT } = require('./socketEvents');
const { findContactUserIds } = require('../service/ChatService');
const { userRoom } = require('../constants/room.constants');
const { logger } = require('../config/logger');

/**
 * Track user coming online.
 * Delegates presence storage to PresenceService.
 * If user just came online, broadcasts presence to active contacts.
 */
const trackConnect = async (io, socket) => {
    const userId = socket.user?.id;
    const socketId = socket.id;

    const { isNewlyOnline } = await PresenceService.addConnection(userId, socketId);
    if (isNewlyOnline) {
        // User just came online — broadcast to contacts
        const contactIds = await findContactUserIds(userId);
        for (const contactId of contactIds) {
            io.to(userRoom(contactId)).emit(USER_PRESENCE_EVENT, {
                userId,
                online: true,
            });
        }
    }
};

/**
 * Track user going offline.
 * Delegates presence removal to PresenceService.
 * If user went fully offline (no other active sockets), broadcasts offline event with lastSeen.
 */
const trackDisconnect = async (io, socket) => {
    const userId = socket.user?.id;
    const socketId = socket.id;

    const { isNowOffline, lastSeen } = await PresenceService.removeConnection(userId, socketId);
    if (isNowOffline) {
        const contactIds = await findContactUserIds(userId);
        for (const contactId of contactIds) {
            io.to(userRoom(contactId)).emit(USER_PRESENCE_EVENT, {
                userId,
                online: false,
                lastSeen,
            });
        }
    }
};

/**
 * Register presence-query event handler on a socket.
 * Client sends an array of userIds, server responds with bulk presence.
 */
const setupPresenceHandler = (io, socket) => {
    socket.on(PRESENCE_QUERY_EVENT, async (data = {}, ack) => {
        try {
            const { userIds } = data;
            if (!Array.isArray(userIds) || userIds.length === 0) {
                return ack?.({});
            }

            // Cap at 500 to prevent abuse
            const capped = userIds.slice(0, 500);
            const presenceMap = await PresenceService.getUsersPresence(capped);
            ack?.(presenceMap);
        } catch (err) {
            logger.error(`Presence query failed for ${socket.user?.id}:`, err.message);
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
            const activeUserIds = new Set();

            for (const s of sockets) {
                const userId = s.user?.id;
                if (userId) {
                    activeUserIds.add(userId);
                }
            }

            if (activeUserIds.size > 0) {
                await PresenceService.refreshHeartbeat(Array.from(activeUserIds));
                logger.info(`Presence heartbeat: refreshed TTL for ${activeUserIds.size} users`);
            }
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
    setupPresenceHandler,
    startPresenceHeartbeat,
};
