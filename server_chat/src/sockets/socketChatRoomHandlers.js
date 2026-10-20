const ChatService = require('../service/ChatService');
const {JOIN_CHAT_EVENT, LEAVE_CHAT_EVENT} = require('./socketEvents');
const {getRoom} = require('./socketRoomManager');
const {buildErrorResponse} = require('../utils/ResponseUtils');
const {isValidUuid} = require('../utils/UuidUtils');
const {logger} = require('../config/logger');
const {getPresence} = require('./socketPresenceHandler');

const setupChatRoomHandlers = (io, socket) => {
    const user = socket.user;

    /**
     * Client emits 'join-chat' when opening a conversation.
     * Server verifies the user is a participant before joining the room.
     */
    socket.on(JOIN_CHAT_EVENT, async ({chatId} = {}, ack) => {
        try {
            if (!chatId || !isValidUuid(chatId)) {
                return ack?.(buildErrorResponse('Invalid chat ID'));
            }

            // Verify user is authorized to access this chat
            await ChatService.checkChatParticipant(chatId, user.id);

            const roomId = getRoom(chatId);
            socket.join(roomId);
            logger.info(`${user.name} joined room ${roomId}`);

            // Fetch presence for chat participants to include in the ack
            const participants = await ChatService.findChatParticipantsByChatId(chatId);
            const participantUserIds = participants.map(p => p.userId).filter(uid => uid !== user.id);
            const presence = await getPresence(participantUserIds);
            ack?.({success: true, presence});
        } catch (err) {
            logger.warn(`${user.name} failed to join chat ${chatId}: ${err.message}`);
            ack?.(buildErrorResponse(err.message));
        }
    });

    /**
     * Client emits 'leave-chat' when switching away from or closing a conversation.
     * No authorization check needed — leaving a room you're not in is a no-op.
     */
    socket.on(LEAVE_CHAT_EVENT, ({chatId} = {}, ack) => {
        try {
            if (!chatId) {
                return ack?.(buildErrorResponse('Chat ID required'));
            }

            const roomId = getRoom(chatId);
            socket.leave(roomId);
            logger.info(`${user.name} left room ${roomId}`);

            ack?.({success: true});
        } catch (err) {
            logger.warn(`${user.name} failed to leave chat ${chatId}: ${err.message}`);
            ack?.(buildErrorResponse(err.message));
        }
    });
};

module.exports = setupChatRoomHandlers;
