const RealtimeService = require('../service/RealtimeService');
const {chatRoom} = require('../constants/room.constants');

// Alias for backward compatibility — existing consumers import getRoom
const getRoom = chatRoom;

const handleGroupMessage = async (io, chatId, eventName) => {
    if (io) {
        RealtimeService.init(io);
    }
    await RealtimeService.notifyGroupEvent(chatId, eventName);
};

module.exports = {
    getRoom,
    handleGroupMessage,
};
