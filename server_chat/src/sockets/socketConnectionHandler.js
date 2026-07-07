const {getRoom, addSocketToRoom} = require('./socketRoomManager');
const {findAllChatParticipantsByUserId} = require('../service/ChatService'); // Direct import from ChatService

const socketConnectionHandler = async (io, socket) => {
    const user = socket.user;

    // Join a personal room for direct cross-node emits
    socket.join(`user_${user.id}`);
    console.info(`Connected: ${user?.name} (Socket ID: ${socket.id})`);

    /* join user rooms */
    const chatParticipants = await findAllChatParticipantsByUserId(user.id);
    chatParticipants.forEach(participants => {
        const roomId = getRoom(participants.chatId);
        addSocketToRoom(socket, roomId);
    });

    socket.on('disconnect', () => {
        console.info(`Disconnected: ${user.name}`);
    });
};

module.exports = socketConnectionHandler;
