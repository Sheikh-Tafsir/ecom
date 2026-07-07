const socketIO = require('socket.io');
const { createAdapter } = require('@socket.io/redis-adapter');
const RedisConfig = require('../config/RedisConfig'); // Assuming RedisConfig is already an instance of ioredis

const socketAuthMiddleware = require('./socketAuthMiddleware');
const socketConnectionHandler = require('./socketConnectionHandler');
const socketMessageHandlers = require('./socketMessageHandlers');
const socketGroupHandlers = require('./socketGroupHandlers');

module.exports = (server) => {
    const io = socketIO(server);

    // Setup Redis Adapter for horizontal scaling
    const pubClient = RedisConfig.duplicate();
    const subClient = RedisConfig.duplicate();

    pubClient.on('error', (err) => console.error('❌ Redis PubClient error:', err));
    subClient.on('error', (err) => console.error('❌ Redis SubClient error:', err));

    io.adapter(createAdapter(pubClient, subClient));

    // Auth middleware
    io.use(socketAuthMiddleware);

    io.on('connection', async (socket) => {
        // Connection handling
        await socketConnectionHandler(io, socket);

        // Message handlers
        socketMessageHandlers(io, socket);

        // Group handlers
        socketGroupHandlers(io, socket);
    });
};
