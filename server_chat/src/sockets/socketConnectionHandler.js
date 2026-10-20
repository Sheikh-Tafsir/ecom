const {logger} = require('../config/logger');
const RedisConfig = require('../config/RedisConfig');
const {trackConnect, trackDisconnect} = require('./socketPresenceHandler');

const USER_CACHE_PREFIX = 'cache:user:';
const USER_CACHE_TTL = 600; // 10 minutes — matches UserService.js

const socketConnectionHandler = async (io, socket) => {
    const user = socket.user;

    // Join a personal room for direct cross-node emits
    socket.join(`user_${user.id}`);
    logger.info(`Connected: ${user?.name} (Socket ID: ${socket.id})`);

    // Track user presence in Redis
    await trackConnect(io, socket);

    // Refresh the Redis user cache from JWT claims on every connect.
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
        const existingRaw = await RedisConfig.get(`${USER_CACHE_PREFIX}${user.id}`);
        if (existingRaw) {
            const existing = JSON.parse(existingRaw);
            profile.image = existing.image || null;
        }

        await RedisConfig.set(
            `${USER_CACHE_PREFIX}${user.id}`,
            JSON.stringify(profile),
            'EX',
            USER_CACHE_TTL
        );
    } catch (err) {
        logger.warn(`Failed to refresh user cache from JWT for ${user.id}:`, err.message);
    }

    // Join admin room if user has administrative permissions
    const permissions = Array.isArray(user?.permissions)
        ? user.permissions
        : (user?.permissions ? [user.permissions] : []);
    if (permissions.includes('admin:access') || permissions.includes('super_admin:access')) {
        socket.join('admin_room');
    }

    // Join permission rooms dynamically for permission-based broadcasts
    permissions.forEach(perm => {
        if (perm) {
            socket.join(`perm_${perm}`);
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
