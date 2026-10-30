const {Op} = require('sequelize');
const xss = require('xss');
const sequelize = require('../config/SequelizeConfig');
const {ChatParticipant, Message, MessageReceipt} = require('../model');
const RuntimeError = require('../common/RuntimeError');
const ApiResponse = require('../common/ApiResponse');
const { generateUuidV7 } = require('../utils/UuidUtils');
const {
    findChatByChatIdAndUserId,
    findOrCreateDirectChat,
    findChatParticipantsByChatId,
    getChatType,
} = require('./ChatService');
const BlockService = require('./BlockService');
const RateLimiterService = require('./RateLimiterService');
const { MESSAGE_RECEIVE_EVENT, CHAT_UPDATED_EVENT } = require('../sockets/socketEvents');
const { getRoom } = require('../sockets/socketRoomManager');
const { userRoom } = require('../constants/room.constants');
const { MAX_MESSAGE_LENGTH } = require('../constants/chat.constants');
const { CHAT_TYPE } = require('../utils/Enum');
const { RECEIVED } = require('../utils/Messages');
const { logger } = require('../config/logger');

const sendMessage = async (senderId, body) => {
    const {chatId, receiverId, content, contentType, tempId} = body;

    if (!content) {
        throw new RuntimeError(400, "Message content cannot be empty.");
    }

    const t = await sequelize.transaction();

    try {
        const chat = chatId
            ? await findChatByChatIdAndUserId(chatId, senderId, t)
            : await findOrCreateDirectChat(senderId, receiverId, t);

        const sanitizedContent = xss(content);

        const message = await Message.create(
            {
                id: generateUuidV7(),
                chatId: chat.id,
                senderId,
                content: sanitizedContent,
                contentType,
            },
            {transaction: t}
        );

        // Store sanitized preview to prevent stored XSS in chat list previews
        await chat.update(
            {lastSent: new Date(), lastMessage: sanitizedContent, lastSenderId: senderId},
            {transaction: t}
        );

        await ChatParticipant.increment(
            {unreadMessage: 1},
            {
                where: {
                    chatId: chat.id,
                    userId: {[Op.ne]: senderId},
                },
                transaction: t,
            }
        );

        // Create delivery receipts in the same transaction
        const participants = await findChatParticipantsByChatId(chat.id);
        const now = new Date();
        const receipts = participants
            .filter(p => String(p.userId) !== String(senderId))
            .map(p => ({
                id: generateUuidV7(),
                messageId: message.id,
                userId: p.userId,
                deliveredAt: now,
                readAt: null,
            }));

        if (receipts.length > 0) {
            await MessageReceipt.bulkCreate(receipts, {
                ignoreDuplicates: true,
                transaction: t,
            });
        }

        await t.commit();

        return { message, participants };

    } catch (err) {
        await t.rollback();
        throw err;
    }
};

/**
 * processAndSendMessage — End-to-end message sending orchestration:
 *
 * 1. Checks message sending rate limits (RateLimiterService -> CacheService)
 * 2. Validates content non-emptiness, maximum length, and valid contentType
 * 3. Enforces user blocking rules (BlockService)
 * 4. Persists the message and delivery receipts atomically (sendMessage)
 * 5. Dispatches real-time Socket.IO events (room broadcast + participant sidebar updates)
 * 6. Returns the formatted message DTO
 *
 * @param {Object} user - The authenticated user entity ({ id, name, ... })
 * @param {Object} body - Request payload ({ chatId, receiverId, content, contentType, tempId })
 * @param {Object|null} io - Socket.IO server instance for real-time fan-out
 * @returns {Promise<Object>} Formatted message payload
 */
const processAndSendMessage = async (user, body, io = null) => {
    const userId = user?.id;
    const chatId = body?.chatId;
    const receiverId = body?.receiverId;
    const content = body?.content;
    const contentType = body?.contentType || 'text';
    const tempId = body?.tempId;

    if (!chatId && !receiverId) {
        throw new RuntimeError(400, "Either chatId or receiverId must be provided.");
    }

    // 1. Rate limiting check via RateLimiterService
    if (await RateLimiterService.isMessageRateLimited(userId)) {
        throw new RuntimeError(429, "Rate limit exceeded. Slow down.");
    }

    // 2. Content validation
    if (!content || typeof content !== 'string' || content.trim().length === 0 || content.length > MAX_MESSAGE_LENGTH) {
        throw new RuntimeError(400, "Message content cannot be empty or exceed maximum length.");
    }

    if (!['text', 'image', 'file'].includes(contentType)) {
        throw new RuntimeError(400, "Invalid content type.");
    }

    // 3. Block checks
    if (receiverId) {
        if (await BlockService.isBlocked(userId, receiverId)) {
            throw new RuntimeError(403, "Cannot send message — user is blocked.");
        }
    } else if (chatId) {
        const chatType = await getChatType(chatId);
        if (chatType === CHAT_TYPE.DIRECT) {
            const participants = await findChatParticipantsByChatId(chatId);
            const otherUser = participants.find(p => String(p.userId) !== String(userId));
            if (otherUser && await BlockService.isBlocked(userId, otherUser.userId)) {
                throw new RuntimeError(403, "Cannot send message — user is blocked.");
            }
        }
    }

    // 4. Atomic persistence in PostgreSQL
    const { message, participants } = await sendMessage(userId, {
        chatId,
        receiverId,
        content: content.trim(),
        contentType,
        tempId,
    });

    const messageData = message.toJSON ? message.toJSON() : message;
    if (tempId) {
        messageData.tempId = tempId;
    }

    // 5. Real-time broadcast via Socket.IO
    if (io) {
        // Active chat room broadcast
        const roomId = getRoom(message.chatId);
        io.to(roomId).emit(MESSAGE_RECEIVE_EVENT, ApiResponse({
            message: RECEIVED,
            data: messageData,
        }));

        // Participant sidebar updates
        const chatUpdate = {
            chatId: message.chatId,
            lastMessage: messageData.content,
            lastSenderId: messageData.senderId,
            lastSent: messageData.createdAt,
            lastSentId: messageData.id,
            contentType: messageData.contentType,
            ...(tempId ? { tempId } : {}),
        };

        for (const p of participants) {
            io.to(userRoom(p.userId)).emit(CHAT_UPDATED_EVENT, ApiResponse({
                message: RECEIVED,
                data: chatUpdate,
            }));
        }
    }

    logger.info(`Message sent from ${user?.name || userId} (${userId}) in chat ${message.chatId}`);

    return messageData;
};

module.exports = {
    sendMessage,
    processAndSendMessage,
};
