const {Op} = require('sequelize');
const xss = require('xss');
const sequelize = require('../config/SequelizeConfig');
const {ChatParticipant, Message, MessageReceipt} = require('../model');
const RuntimeError = require('../common/RuntimeError');
const {
    findChatByChatIdAndUserId,
    findOrCreateDirectChat,
    findChatParticipantsByChatId
} = require('./ChatService');

const sendMessage = async (senderId, body) => {
    const {chatId, receiverId, content, contentType, tempId} = body;

    const t = await sequelize.transaction();

    try {
        const chat = chatId
            ? await findChatByChatIdAndUserId(chatId, senderId, t)
            : await findOrCreateDirectChat(senderId, receiverId, t);

        const sanitizedContent = xss(content);

        const message = await Message.create(
            {
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

        await t.commit();

        return message;

    } catch (err) {
        await t.rollback();
        throw err;
    }
};

const saveMessageReceipts = async (activeUsers = [], messageId, chatId, senderId) => {
    // activeUsers is kept for backward compatibility but is intentionally unused here.
    // Read receipts (readAt) must only be set when the user explicitly views the chat
    // via a 'mark-as-read' event. Being connected via socket does not imply the user
    // is actively viewing this specific chat window.
    const now = Date.now();

    const allParticipants = await findChatParticipantsByChatId(chatId);

    const receipts = allParticipants
        .filter(participant => participant.userId != senderId)
        .map(participant => {
            const dateNow = new Date(now);
            return {
                messageId,
                userId: participant.userId,
                deliveredAt: dateNow,
                readAt: null
            };
        }
    );

    const t = await sequelize.transaction();

    try {
        const result = await MessageReceipt.bulkCreate(receipts, {
            ignoreDuplicates: true,
            transaction: t
        });

        await t.commit();
        return result;

    } catch (err) {
        await t.rollback();
        throw err;
    }
}

module.exports = {
    sendMessage,
    saveMessageReceipts,
};
