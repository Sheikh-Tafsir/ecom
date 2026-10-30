const {logger} = require('../config/logger');
const CacheService = require('../service/CacheService');
const {trackConnect, trackDisconnect} = require('./socketPresenceHandler');
const {Permission} = require('../utils/Enum');
const {USER_CACHE_PREFIX, USER_CACHE_TTL} = require('../constants/redis.constants');
const {userRoom, permRoom, ADMIN_ROOM} = require('../constants/room.constants');

const socketConnectionHandler = async (io, socket) => {
    const user = socket.user;

    // Join a personal room for direct cross-node emits
    socket.join(userRoom(user.id));
    logger.info(`Connected: ${user?.name} (Socket ID: ${socket.id})`);

    // Track user presence in Redis
    await trackConnect(io, socket);

    // Refresh the user cache from JWT claims on every connect.
    // This keeps user name/email fresh without an HTTP call to the main server.
    // Staleness window = JWT access token TTL (15 min) — acceptable for chat display names.
    try {
        const profile = {
            id: user.id,
            name: user.name || 'User',
            image: null,
            email: user.email || null,
        };

        // Merge with existing cache to preserve fields not in JWT (e.g. image)
        const existingRaw = await CacheService.get(`${USER_CACHE_PREFIX}${user.id}`);
        if (existingRaw) {
            const existing = JSON.parse(existingRaw);
            profile.image = existing.image || null;
        }

        await CacheService.set(
            `${USER_CACHE_PREFIX}${user.id}`,
            JSON.stringify(profile),
            USER_CACHE_TTL
        );
    } catch (err) {
        logger.warn(`Failed to refresh user cache from JWT for ${user.id}:`, err.message);
    }

    // Join admin room if user has administrative permissions
    const permissions = Array.isArray(user?.permissions)
        ? user.permissions
        : (user?.permissions ? [user.permissions] : []);
    if (permissions.includes(Permission.ADMIN_ACCESS) || permissions.includes(Permission.SUPER_ADMIN_ACCESS)) {
        socket.join(ADMIN_ROOM);
    }

    // Join permission rooms dynamically for permission-based broadcasts
    permissions.forEach(perm => {
        if (perm) {
            socket.join(permRoom(perm));
        }
    });

    // Chat rooms are joined on demand via 'join-chat' event (hybrid architecture).
    // No DB query or bulk room joins needed on connect.

    socket.on('disconnect', async () => {
        logger.info(`Disconnected: ${user.name}`);
        await trackDisconnect(io, socket);
    });
};

module.exports = socketConnectionHandler;
