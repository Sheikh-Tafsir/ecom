const {ChatParticipant} = require('../model');
const {getSockets} = require('./socketManager');
const ApiResponse = require('../common/ApiResponse');

const getRoom = (chatId) => `chat_${chatId}`;

const handleGroupMessage = async (io, chatId, eventName) => {
    const roomId = getRoom(chatId);
    const participants = await ChatParticipant.findAll({
        where: {chatId},
        attributes: ['userId'],
        raw: true
    });

    participants.forEach(participant => {
        const sockets = getSockets(participant.userId);

        sockets.forEach(socket => {
            addSocketToRoom(socket, roomId);
            io.to(socket.id).emit(eventName, ApiResponse({
                message: "Group event",
                data: chatId
            }));
        });
    });
};

const getActiveUsersInRoom = (io, roomId) => {
    const socketIds = io.sockets.adapter.rooms.get(roomId);
    if (!socketIds) return [];

    const activeUserIds = new Set();
    socketIds.forEach((socketId) => {
        const socket = io.sockets.sockets.get(socketId);
        if (socket?.user?.id) {
            activeUserIds.add(socket.user.id);
        }
    });

    return Array.from(activeUserIds);
}

const addSocketToRoom = (socket, roomId) => {
    if (!socket.rooms.has(roomId)) {
        socket.join(roomId);
    }
}

module.exports = {
    getRoom,
    handleGroupMessage,
    getActiveUsersInRoom,
    addSocketToRoom,
};
