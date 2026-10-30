const CacheService = require('../service/CacheService');
const {logger} = require('../config/logger');
const {TYPING_EVENT, TYPING_BROADCAST_EVENT} = require('./socketEvents');
const {getRoom} = require('./socketRoomManager');
const ChatService = require('../service/ChatService');
const BlockService = require('../service/BlockService');
const {CHAT_TYPE} = require('../utils/Enum');

const TYPING_THROTTLE_PREFIX = 'typing:';
const TYPING_THROTTLE_TTL = 2; // seconds

const setupTypingHandler = (io, socket) => {
    const user = socket.user;

    socket.on(TYPING_EVENT, async (data = {}) => {
        try {
            const {chatId} = data;
            if (!chatId) return;

            const roomId = getRoom(chatId);

            // Auth check: only broadcast if the user has joined this chat room (zero DB cost)
            if (!socket.rooms.has(roomId)) return;

            // Cache throttle: NX ensures only one event per 2 seconds per user per chat
            const throttleKey = `${TYPING_THROTTLE_PREFIX}${chatId}:${user.id}`;
            const set = await CacheService.set(throttleKey, '1', 'EX', TYPING_THROTTLE_TTL, 'NX');
            if (!set) return; // Throttled

            // Block check for DMs: don't broadcast typing to a blocked user
            const chatType = await ChatService.getChatType(chatId);
            if (chatType === CHAT_TYPE.DIRECT) {
                const participants = await ChatService.findChatParticipantsByChatId(chatId);
                const otherUser = participants.find(p => p.userId !== user.id);
                if (otherUser && await BlockService.isBlocked(user.id, otherUser.userId)) {
                    return; // Silently drop — blocked
                }
            }

            // Broadcast to chat room excluding sender
            socket.to(roomId).emit(TYPING_BROADCAST_EVENT, {
                chatId,
                userId: user.id,
                userName: user.name,
            });
        } catch (err) {
            logger.error(`Typing handler error for ${user.id}:`, err.message);
        }
    });
};

module.exports = setupTypingHandler;
