// Socket.IO room name builders
const userRoom = (userId) => `user_${userId}`;
const chatRoom = (chatId) => `chat_${chatId}`;
const permRoom = (perm) => `perm_${perm}`;

const ADMIN_ROOM = 'admin_room';

module.exports = {
    userRoom,
    chatRoom,
    permRoom,
    ADMIN_ROOM,
};
