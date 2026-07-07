const {ChatParticipant} = require('../model');
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
        io.in(`user_${participant.userId}`).socketsJoin(roomId);
    });

    io.to(roomId).emit(eventName, ApiResponse({
        message: "Group event",
        data: chatId
    }));
};

const getActiveUsersInRoom = async (io, roomId) => {
    const sockets = await io.in(roomId).fetchSockets();
    if (!sockets) return [];

    const activeUserIds = new Set();
    for (const socket of sockets) {
        if (socket?.user?.id) {
            activeUserIds.add(socket.user.id);
        }
    }

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
