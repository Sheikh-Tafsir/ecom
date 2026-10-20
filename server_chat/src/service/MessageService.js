const {Op} = require('sequelize');
const xss = require('xss');
const sequelize = require('../config/SequelizeConfig');
const {ChatParticipant, Message, MessageReceipt} = require('../model');
const RuntimeError = require('../common/RuntimeError');
const { generateUuidV7 } = require('../utils/UuidUtils');
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

module.exports = {
    sendMessage,
};
