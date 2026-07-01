const {Op} = require('sequelize');
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

        const message = await Message.create(
            {
                chatId: chat.id,
                senderId,
                content,
                contentType,
            },
            {transaction: t}
        );

        await chat.update(
            {lastSent: new Date(), lastMessage: content},
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
    const now = Date.now();

    const allParticipants = await findChatParticipantsByChatId(chatId);
    const activeSet = new Set(activeUsers.map(String));

    const receipts = allParticipants
        .filter(participant => participant.userId != senderId)
        .map(participant => {
            const isActive = activeSet.has(String(participant.userId));

            return {
                messageId,
                userId: participant.userId,
                deliveredAt: now,
                read_at: isActive ? now : null
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

const seenChatMessage = async (chatId, body, userId) => {
    const {lastSeen} = body;

    const t = await sequelize.transaction();

    try {
        const participant = await ChatParticipant.findOne({
            where: {chatId, userId},
            transaction: t,
        });

        if (!participant) {
            throw new RuntimeError(403, "Not a participant");
        }

        await participant.update(
            {
                unreadMessage: 0,
                lastSeen,
            },
            {transaction: t}
        );

        await t.commit();
    } catch (err) {
        await t.rollback();
        throw err;
    }
};


module.exports = {
    sendMessage,
    saveMessageReceipts,
    seenChatMessage,
};
