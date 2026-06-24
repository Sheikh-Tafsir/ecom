const MAX_SOCKETS = 3;

const userSockets = new Map();

const addSocket = (userId, socket) => {
    const sockets = userSockets.get(userId) || [];

    if (sockets.length >= MAX_SOCKETS) {
        sockets[0]?.disconnect();
    }

    if (!sockets.find(s => s.id === socket.id)) {
        sockets.push(socket);
    }
    userSockets.set(userId, sockets);
};

const removeSocket = (userId, socketId) => {
    const sockets = userSockets.get(userId) || [];

    const filtered = sockets.filter(s => s.id !== socketId);

    if (!filtered.length) {
        userSockets.delete(userId);
    } else {
        userSockets.set(userId, filtered);
    }
};

const getSockets = (userId) => userSockets.get(userId) || [];

module.exports = {
    addSocket,
    removeSocket,
    getSockets,
};