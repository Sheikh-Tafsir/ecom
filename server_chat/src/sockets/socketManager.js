const MAX_SOCKETS = 3;

const userSocketMap = new Map();

const addSocket = (userId, socket) => {
    const sockets = userSocketMap.get(userId) || [];

    if (sockets.length >= MAX_SOCKETS) {
        const old = sockets.shift();
        old?.disconnect();
    }

    sockets.push(socket);
    userSocketMap.set(userId, sockets);
};

const removeSocket = (userId, socketId) => {
    const sockets = userSocketMap.get(userId) || [];

    const filtered = sockets.filter(s => s.id !== socketId);

    if (!filtered.length) {
        userSocketMap.delete(userId);
    } else {
        userSocketMap.set(userId, filtered);
    }
};

const getSockets = (userId) => userSocketMap.get(userId) || [];

module.exports = {
    addSocket,
    removeSocket,
    getSockets,
};