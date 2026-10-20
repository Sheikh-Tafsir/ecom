const {ChatParticipant} = require('../model');
const ApiResponse = require('../common/ApiResponse');

const getRoom = (chatId) => `chat_${chatId}`;

const handleGroupMessage = async (io, chatId, eventName) => {
    const participants = await ChatParticipant.findAll({
        where: {chatId},
        attributes: ['userId'],
        raw: true
    });

    // Group events (create/update) are sidebar-level — emit to each participant's personal room
    for (const participant of participants) {
        io.to(`user_${participant.userId}`).emit(eventName, ApiResponse({
            message: "Group event",
            data: chatId
        }));
    }
};

module.exports = {
    getRoom,
    handleGroupMessage,
};
