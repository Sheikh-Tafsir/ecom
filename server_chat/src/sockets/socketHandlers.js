const socketIO = require('socket.io');
const socketAuthMiddleware = require('./socketAuthMiddleware');
const socketConnectionHandler = require('./socketConnectionHandler');
const socketMessageHandlers = require('./socketMessageHandlers');
const socketGroupHandlers = require('./socketGroupHandlers');

module.exports = (server) => {
    const io = socketIO(server);

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
