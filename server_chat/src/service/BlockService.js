const {Op} = require('sequelize');
const {BlockedUser} = require('../model');
const RuntimeError = require('../common/RuntimeError');
const {generateUuidV7} = require('../utils/UuidUtils');

/**
 * Block a user. Creates a record in blocked_users.
 */
const blockUser = async (userId, blockedUserId) => {
    if (userId === blockedUserId) {
        throw new RuntimeError(400, 'Cannot block yourself');
    }

    const existing = await BlockedUser.findOne({
        where: {userId, blockedUserId},
    });

    if (existing) {
        throw new RuntimeError(409, 'User is already blocked');
    }

    await BlockedUser.create({
        id: generateUuidV7(),
        userId,
        blockedUserId,
    });
};

/**
 * Unblock a user. Deletes the record from blocked_users.
 */
const unblockUser = async (userId, blockedUserId) => {
    const deleted = await BlockedUser.destroy({
        where: {userId, blockedUserId},
    });

    if (deleted === 0) {
        throw new RuntimeError(404, 'User is not blocked');
    }
};

/**
 * Check block status between two users (bidirectional).
 * Returns { blocked, blockedByMe, blockedByOther }
 */
const getBlockStatus = async (userId, otherUserId) => {
    const records = await BlockedUser.findAll({
        where: {
            [Op.or]: [
                {userId, blockedUserId: otherUserId},
                {userId: otherUserId, blockedUserId: userId},
            ],
        },
        attributes: ['userId', 'blockedUserId'],
        raw: true,
    });

    const blockedByMe = records.some(r => r.userId === userId && r.blockedUserId === otherUserId);
    const blockedByOther = records.some(r => r.userId === otherUserId && r.blockedUserId === userId);

    return {
        blocked: blockedByMe || blockedByOther,
        blockedByMe,
        blockedByOther,
    };
};

/**
 * Check if either user has blocked the other (for message guard).
 */
const isBlocked = async (userId1, userId2) => {
    const count = await BlockedUser.count({
        where: {
            [Op.or]: [
                {userId: userId1, blockedUserId: userId2},
                {userId: userId2, blockedUserId: userId1},
            ],
        },
    });
    return count > 0;
};

module.exports = {
    blockUser,
    unblockUser,
    getBlockStatus,
    isBlocked,
};
