const {getRoom, addSocketToRoom} = require('./socketRoomManager');
const {findAllChatParticipantsByUserId} = require('../service/ChatService'); // Direct import from ChatService
const {logger} = require('../config/logger');

const socketConnectionHandler = async (io, socket) => {
    const user = socket.user;

    // Join a personal room for direct cross-node emits
    socket.join(`user_${user.id}`);
    logger.info(`Connected: ${user?.name} (Socket ID: ${socket.id})`);

    // Join admin room if user has administrative permissions
    const permissions = Array.isArray(user?.permissions)
        ? user.permissions
        : (user?.permissions ? [user.permissions] : []);
    if (permissions.includes('admin:access') || permissions.includes('super_admin:access')) {
        socket.join('admin_room');
    }

    /* join user rooms */
    const chatParticipants = await findAllChatParticipantsByUserId(user.id);
    chatParticipants.forEach(participants => {
        const roomId = getRoom(participants.chatId);
        addSocketToRoom(socket, roomId);
    });

    socket.on('disconnect', () => {
        logger.info(`Disconnected: ${user.name}`);
    });
};

module.exports = socketConnectionHandler;
