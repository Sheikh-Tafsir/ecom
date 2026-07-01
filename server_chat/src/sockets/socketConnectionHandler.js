const {addSocket, removeSocket} = require('./socketManager');
const {getRoom, addSocketToRoom} = require('./socketRoomManager');
const {findAllChatParticipantsByUserId} = require('../service/ChatService'); // Direct import from ChatService

const socketConnectionHandler = async (io, socket) => {
    const user = socket.user;

    addSocket(user.id, socket);
    console.info(`Connected: ${user?.name}`);

    /* join user rooms */
    const chatParticipants = await findAllChatParticipantsByUserId(user.id);
    chatParticipants.forEach(participants => {
        const roomId = getRoom(participants.chatId);
        addSocketToRoom(socket, roomId);
    });

    socket.on('disconnect', () => {
        removeSocket(user.id, socket.id);
        console.info(`Disconnected: ${user.name}`);
    });
};

module.exports = socketConnectionHandler;
