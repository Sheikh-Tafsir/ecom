const MAX_SOCKETS = 3;

const userSockets = new Map();
const roomUsers = new Map();

const addSocket = (userId, socket) => {
    const sockets = userSockets.get(userId) || [];

    if (sockets.length >= MAX_SOCKETS) {
        const old = sockets.shift();
        old?.disconnect();
    }

    sockets.push(socket);
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

const addUserToRoom = (roomId, userId) => {
    if (!roomUsers.has(roomId)) {
        roomUsers.set(roomId, new Set());
    }
    roomUsers.get(roomId).add(userId);
};

const removeUserFromAllRooms = (userId) => {
    for (const [roomId, users] of roomUsers.entries()) {
        users.delete(userId);

        if (users.size === 0) {
            roomUsers.delete(roomId);
        }
    }
};

const getUsersInRoom = (roomId) => {
    return Array.from(roomUsers.get(roomId) || []);
};

module.exports = {
    addSocket,
    removeSocket,
    getSockets,
    addUserToRoom,
    removeUserFromAllRooms,
    getUsersInRoom
};