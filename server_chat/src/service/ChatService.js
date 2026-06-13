const {Op} = require('sequelize');

const sequelize = require('../config/SequelizeConfig');
const {User, Chat, ChatParticipant, Message} = require('../model');

const {NOT_FOUND} = require('../utils/Messages');
const {CHAT_TYPE, CHAT_MEMBER_TYPE} = require('../utils/Enum');

const RuntimeError = require('../common/RuntimeError');
const ApiResponse = require("../common/ApiResponse");

const CHAT_LIST_DEFAULT_SIZE = 15;
const CHAT_MESSAGE_DEFAULT_SIZE = 15;

const getAllChatsByUserId = async (filters = {}, userId) => {
    const {
        type,
        cursorLastSent,
        cursorId,
    } = filters;

    const where = {};
    if (type) where.type = type;

    if (cursorLastSent && cursorId) {
        where[Op.or] = [
            {
                lastSent: {
                    [Op.lt]: cursorLastSent
                }
            },
            {
                lastSent: cursorLastSent,
                id: {
                    [Op.lt]: cursorId
                }
            }
        ];
    }

    const limit = CHAT_LIST_DEFAULT_SIZE;

    const chats = await Chat.findAll({
        where,
        include: [
            {
                model: ChatParticipant,
                as: "Participants",
                where: { userId },
                required: true,
                attributes: []
            }
        ],
        order: [
            ["lastSent", "DESC"],
            ["id", "DESC"],
        ],
        limit: limit + 1,
    });

    const hasMore = chats.length > limit;

    const pageChats = hasMore
        ? chats.slice(0, limit)
        : chats;

    const nextCursor =
        hasMore && pageChats.length
            ? pageChats[pageChats.length - 1].id
            : null;

    return {
        chats: pageChats,
        pagination: {
            hasMore,
            nextCursor,
            limit,
        },
    };
};


const getChatById = async (id, filters = {}, userId) => {
    const membership = await ChatParticipant.findOne({
        where: {
            chatId: id,
            userId,
        },
        attributes: ["role"],
    });

    if (!membership) {
        throw new RuntimeError(404, NOT_FOUND);
    }

    const {
        cursorCreatedAt,
        cursorId,
    } = filters;

    const limit = CHAT_MESSAGE_DEFAULT_SIZE;

    const [chat, messages] = await Promise.all([
        Chat.findByPk(id, {
            include: [
                {
                    model: ChatParticipant,
                    as: "Participants",
                    attributes: ["userId", "role"],
                    include: [
                        {
                            model: User,
                            as: "User",
                            attributes: ["id", "name", "image"],
                        },
                    ],
                },
            ],
        }),

        Message.findAll({
            where: {
                chatId: id,
                ...(cursorCreatedAt && cursorId && {
                    [Op.or]: [
                        {
                            createdAt: {
                                [Op.lt]: cursorCreatedAt,
                            },
                        },
                        {
                            createdAt: cursorCreatedAt,
                            id: {
                                [Op.lt]: cursorId,
                            },
                        },
                    ],
                }),
            },
            order: [
                ["createdAt", "DESC"],
                ["id", "DESC"],
            ],
            limit: limit + 1,
        })
    ]);

    if (!chat) {
        throw new RuntimeError(404, NOT_FOUND);
    }

    const hasMore = messages.length > limit;

    const pageMessages = hasMore
        ? messages.slice(0, limit)
        : messages;

    const nextCursor =
        hasMore && pageMessages.length
            ? pageMessages[pageMessages.length - 1].id
            : null;

    return {
        ...formatChatDetails(chat, userId),

        messages: pageMessages.reverse(),

        pagination: {
            hasMore,
            nextCursor,
            limit,
        },
    }
};

const formatChatDetails = (chat, userId) => {
    const c = chat.get({plain: true});

    const participants = c.Participants.map(p => ({
        id: p.userId,
        name: p.User?.name,
        image: p.User?.image,
        role: p.role,
    }));

    let name = c.name;

    if (c.type === CHAT_TYPE.DIRECT) {
        const other = participants.find(p => p.id !== userId);
        name = other?.name || "Unknown";
    }

    return {
        id: c.id,
        type: c.type,
        name,
        participants,
        messages: c.Messages,
    };
};

const sendMessage = async (senderId, body) => {
    const {chatId, receiverId, content, contentType} = body;

    if (!content) {
        throw new RuntimeError(400, "Message is required");
    }

    const t = await sequelize.transaction();

    try {
        const chat = chatId
            ? await findChat(chatId, senderId, t)
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
            {lastSent: new Date()},
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

        return {
            chatId: chat.id,
            message: {
                id: message.id,
                chatId: chat.id,
                senderId,
                content,
                contentType,
                createdAt: message.createdAt,
            },
        };

    } catch (err) {
        await t.rollback();
        throw err;
    }
};

/* =========================
   DIRECT CHAT FIND/CREATE
========================= */
const findOrCreateDirectChat = async (user1, user2, t) => {
    const [existingChat] = await sequelize.query(
        `
            SELECT c.id
            FROM product_chats c
                     JOIN product_chatparticipants cp ON cp.chat_id = c.id
            WHERE c.type = 'direct'
              AND cp.user_id IN (:user1, :user2)
            GROUP BY c.id
            HAVING COUNT(DISTINCT cp.user_id) = 2 LIMIT 1
        `,
        {
            replacements: {user1, user2},
            transaction: t,
            type: sequelize.QueryTypes.SELECT
        }
    );

    if (existingChat?.id) {
        return Chat.findByPk(existingChat.id, {transaction: t});
    }

    const chat = await Chat.create(
        {type: CHAT_TYPE.DIRECT},
        {transaction: t}
    );

    await ChatParticipant.bulkCreate(
        [
            {chatId: chat.id, userId: user1, role: CHAT_MEMBER_TYPE.MEMBER},
            {chatId: chat.id, userId: user2, role: CHAT_MEMBER_TYPE.MEMBER},
        ],
        {transaction: t}
    );

    return chat;
};

const findChat = async (chatId, userId, t) => {
    const chat = await Chat.findByPk(chatId, {
        include: [
            {
                model: ChatParticipant,
                as: "Participants",
                attributes: ["userId"],
            },
        ],
        transaction: t,
    });

    if (!chat) throw new RuntimeError(404, NOT_FOUND);

    checkChatParticipant(chat, userId);

    return chat;
};

/* =========================
   SEEN / READ RECEIPTS
========================= */
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

        return ApiResponse({message: "Success"});

    } catch (err) {
        await t.rollback();
        throw err;
    }
};

/* =========================
   GROUP CHAT
========================= */
const createGroup = async (body, user) => {
    const t = await sequelize.transaction();

    try {
        if (!body.users?.length || body.users.length < 2) {
            throw new RuntimeError(400, "Need at least 2 users");
        }

        const name = buildGroupName(user, body.users);

        const chat = await Chat.create(
            {name, type: CHAT_TYPE.GROUP},
            {transaction: t}
        );

        await ChatParticipant.bulkCreate(
            [
                {chatId: chat.id, userId: user.id, role: CHAT_MEMBER_TYPE.ADMIN},
                ...body.users.map(u => ({
                    chatId: chat.id,
                    userId: u.id,
                    role: CHAT_MEMBER_TYPE.MEMBER,
                })),
            ],
            {transaction: t}
        );

        await t.commit();

        return {chatId: chat.id};

    } catch (err) {
        await t.rollback();
        throw err;
    }
};

const buildGroupName = (creator, users) =>
    `${creator.name.split(" ")[0]}, ${users.map(u => u.name.split(" ")[0]).join(", ")}`;

const checkChatParticipant = (chat, userId) => {
    const ids = chat.Participants.map(p => p.userId);

    if (!ids.includes(userId)) {
        throw new RuntimeError(403, "Not a participant");
    }
};

/* =========================
   EXPORTS
========================= */
module.exports = {
    getAllChatsByUserId,
    getChatById,
    sendMessage,
    seenChatMessage,
    createGroup,
};
