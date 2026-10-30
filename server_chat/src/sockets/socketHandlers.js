const socketIO = require('socket.io');
const { createAdapter } = require('@socket.io/redis-adapter');
const RedisConfig = require('../config/RedisConfig'); // Assuming RedisConfig is already an instance of ioredis

const { logger } = require('../config/logger');
const RealtimeService = require('../service/RealtimeService');
const socketAuthMiddleware = require('./socketAuthMiddleware');
const socketConnectionHandler = require('./socketConnectionHandler');
const socketMessageHandlers = require('./socketMessageHandlers');
const socketGroupHandlers = require('./socketGroupHandlers');
const socketChatRoomHandlers = require('./socketChatRoomHandlers');
const {setupPresenceHandler, startPresenceHeartbeat} = require('./socketPresenceHandler');
const setupTypingHandler = require('./socketTypingHandler');
const { initNotificationSubscriber } = require('./socketNotificationHandler');
const { initUserUpdateSubscriber } = require('./socketUserUpdateHandler');

module.exports = (server) => {
    const allowedOrigins = process.env.CORS_ALLOWED_ORIGINS;
    const socketOptions = allowedOrigins
        ? {cors: {origin: allowedOrigins.split(',').map(o => o.trim()), credentials: true}}
        : {};

    const io = socketIO(server, socketOptions);
    RealtimeService.init(io);

    // Setup Redis Adapter for horizontal scaling
    const pubClient = RedisConfig.duplicate();
    const subClient = RedisConfig.duplicate();

    pubClient.on('error', (err) => logger.error('❌ Redis PubClient error:', err));
    subClient.on('error', (err) => logger.error('❌ Redis SubClient error:', err));

    io.adapter(createAdapter(pubClient, subClient));

    // Start presence heartbeat to refresh TTLs
    startPresenceHeartbeat(io);

    // Initialize notification subscriber for cross-service events
    initNotificationSubscriber(io);

    // Initialize user-update subscriber for chat user cache eviction
    initUserUpdateSubscriber();

    // Auth middleware
    io.use(socketAuthMiddleware);

    io.on('connection', async (socket) => {
        // Connection handling
        await socketConnectionHandler(io, socket);

        // Message handlers
        socketMessageHandlers(io, socket);

        // Group handlers
        socketGroupHandlers(io, socket);

        // Chat room handlers (join/leave active conversations)
        socketChatRoomHandlers(io, socket);

        // Presence query handler
        setupPresenceHandler(io, socket);

        // Typing handler
        setupTypingHandler(io, socket);
    });

    return io;
};
