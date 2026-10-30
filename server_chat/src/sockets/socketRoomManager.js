const {ChatParticipant} = require('../model');
const ApiResponse = require('../common/ApiResponse');
const RealtimeService = require('../service/RealtimeService');
const {chatRoom} = require('../constants/room.constants');

// Alias for backward compatibility — existing consumers import getRoom
const getRoom = chatRoom;

const handleGroupMessage = async (io, chatId, eventName) => {
    if (io) {
        RealtimeService.init(io);
    }

    const participants = await ChatParticipant.findAll({
        where: {chatId},
        attributes: ['userId'],
        raw: true
    });

    // Group events (create/update) are sidebar-level — emit to each participant's personal room
    const userIds = participants.map(p => p.userId);
    RealtimeService.emitToUsers(userIds, eventName, ApiResponse({
        message: "Group event",
        data: chatId
    }));
};

module.exports = {
    getRoom,
    handleGroupMessage,
};
