const {Op} = require('sequelize');

const sequelize = require('../config/SequelizeConfig');
const {User, Chat, ChatParticipant, Message, MessageReceipt} = require('../model');

const {NOT_FOUND} = require('../utils/Messages');
const {CHAT_TYPE, CHAT_MEMBER_TYPE} = require('../utils/Enum');

const RuntimeError = require('../common/RuntimeError');

const CHAT_LIST_DEFAULT_SIZE = 15;
const CHAT_MESSAGE_DEFAULT_SIZE = 15;

const findAllChatsByUserId = async (filters = {}, userId) => {
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

    // Get all chat IDs the user is participant of
    const userParticipants = await ChatParticipant.findAll({
        where: { userId },
        attributes: ["chatId"],
        raw: true,
    });

    const chatIds = userParticipants.map(up => up.chatId);

    const chats = await Chat.findAll({
        where: {
            ...where,
            id: chatIds
        },
        include: [
            {
                model: ChatParticipant,
                as: "Participants",
                attributes: ["userId", "role", "unreadMessage"],
                include: [
                    {
                        model: User,
                        as: "User",
                        attributes: ["id", "name", "image"],
                    },
                ],
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

    const formattedChats = pageChats.map(chat => formatChatListDetails(chat, userId));

    const nextCursor =
        hasMore && pageChats.length
            ? pageChats[pageChats.length - 1].id
            : null;

    return {
        chats: formattedChats,
        pagination: {
            hasMore,
            nextCursor,
            limit,
        },
    };
};

const formatChatListDetails = (chat, userId) => {
    const c = chat.get({plain: true});

    const participants = c.Participants.map(p => ({
        id: p.userId,
        userId: p.userId,
        name: p.User?.name,
        image: p.User?.image,
        role: p.role,
    }));

    const currentUserParticipant = c.Participants.find(p => Number(p.userId) === Number(userId));
    const unreadMessage = currentUserParticipant ? currentUserParticipant.unreadMessage : 0;

    let name = c.name;
    let image = c.image;
    let otherUserId = null;
    let shortName = null;

    if (c.type === CHAT_TYPE.DIRECT) {
        const other = participants.find(p => Number(p.id) !== Number(userId));
        name = other?.name || "Unknown";
        image = other?.image;
        otherUserId = other?.id;
        shortName = other?.name ? other.name.split(" ")[0] : "Unknown";
    } else {
        shortName = name;
    }

    return {
        id: c.id,
        type: c.type,
        name,
        image,
        lastMessage: c.lastMessage,
        lastSent: c.lastSent,
        unreadMessage,
        participants,
        Participants: participants,
        otherUserId,
        shortName
    };
};

const findDetailsChatById = async (id, filters = {}, userId) => {
    await checkChatParticipant(id, userId, null)

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
        userId: p.userId,
        name: p.User?.name,
        image: p.User?.image,
        role: p.role,
    }));

    let name = c.name;

    if (c.type === CHAT_TYPE.DIRECT) {
        const other = participants.find(p => Number(p.id) !== Number(userId));
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

const findAllChatParticipantsByUserId = async (userId) => {
    return await ChatParticipant.findAll({
        where: {userId},
        attributes: ['chatId'],
        raw: true,
    });
}

const sendMessage = async (senderId, body) => {
    const {chatId, receiverId, content, contentType, tempId} = body;

    if (!content) {
        throw new RuntimeError(400, "Message is required");
    }

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


/* =========================
   DIRECT CHAT FIND/CREATE
========================= */
const findOrCreateDirectChat = async (user1, user2, transaction) => {
    const [existingChat] = await sequelize.query(
        `
            SELECT c.id
            FROM chats c
                     JOIN chat_participants cp ON cp.chat_id = c.id
            WHERE c.type = 'direct'
              AND cp.user_id IN (:user1, :user2)
            GROUP BY c.id
            HAVING COUNT(DISTINCT cp.user_id) = 2 LIMIT 1
        `,
        {
            replacements: {user1, user2},
            transaction,
            type: sequelize.QueryTypes.SELECT
        }
    );

    if (existingChat) {
        return await findChatById(existingChat?.id, transaction);
    }

    const chat = await Chat.create(
        {type: CHAT_TYPE.DIRECT},
        {transaction}
    );

    await ChatParticipant.bulkCreate(
        [
            {chatId: chat.id, userId: user1, role: CHAT_MEMBER_TYPE.MEMBER},
            {chatId: chat.id, userId: user2, role: CHAT_MEMBER_TYPE.MEMBER},
        ],
        {transaction}
    );

    return chat;
};

const findChatByChatIdAndUserId = async (chatId, userId, transaction) => {
    await checkChatParticipant(chatId, userId, transaction);
    return await findChatById(chatId, transaction);
};

const findChatById = async (chatId, transaction) => {
    const chat = await Chat.findByPk(chatId, {
        attributes: ['id'],
        transaction,
    });

    if (!chat) throw new RuntimeError(404, "Chat with id: " + chatId + " not found");

    return chat;
}

const checkChatParticipant = async (chatId, userId, transaction) => {
    const membership = await ChatParticipant.findOne({
        where: {
            chatId,
            userId,
        },
        attributes: ["role"],
        transaction,
    });

    if (!membership) {
        throw new RuntimeError(404, "User with id: " + userId + " Not a participant of chat with id: " + chatId);
    }
}


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

const findChatParticipantsByChatId = async (chatId) => {
    return await ChatParticipant.findAll({
        where: { chatId },
        attributes: ['userId'],
        raw: true,
    });
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

const updateGroup = async (body, user) => {
    const { chatId, users } = body;

    if (!chatId) {
        throw new RuntimeError(400, "Chat ID is required");
    }

    if (!users?.length) {
        throw new RuntimeError(400, "At least one user is required to add");
    }

    const t = await sequelize.transaction();

    try {
        await checkChatParticipant(chatId, user.id, t);

        await ChatParticipant.bulkCreate(
            users.map(u => ({
                chatId: Number(chatId),
                userId: u.id,
                role: CHAT_MEMBER_TYPE.MEMBER,
            })),
            {
                ignoreDuplicates: true,
                transaction: t
            }
        );

        await t.commit();

        return { chatId };
    } catch (err) {
        await t.rollback();
        throw err;
    }
};

const buildGroupName = (creator, users) =>
    `${creator.name.split(" ")[0]}, ${users.map(u => u.name.split(" ")[0]).join(", ")}`;

/* =========================
   EXPORTS
========================= */
module.exports = {
    findAllChatsByUserId,
    findDetailsChatById,
    findAllChatParticipantsByUserId,
    sendMessage,
    saveMessageReceipts,
    seenChatMessage,
    createGroup,
    updateGroup,
};
