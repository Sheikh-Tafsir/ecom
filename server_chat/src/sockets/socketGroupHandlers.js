const ChatService = require('../service/ChatService'); // Facade for ChatManagementService
const ApiResponse = require('../common/ApiResponse');
const {
    GROUP_CREATE_REQUEST_EVENT,
    GROUP_CREATE_RESPONSE_EVENT,
    GROUP_UPDATE_REQUEST_EVENT,
    GROUP_UPDATE_RESPONSE_EVENT
} = require('./socketEvents');
const {handleGroupMessage} = require('./socketRoomManager');
const {buildErrorResponse} = require("../utils/ResponseUtils");

const setupGroupHandlers = (io, socket) => {
    const user = socket.user;

    socket.on(GROUP_CREATE_REQUEST_EVENT, async (reqBody, ack) => {
        try {
            console.info(`Group create request from ${user.name}:`, reqBody);
            const response = await ChatService.createGroup(reqBody, user);

            ack(ApiResponse({
                message: "Group created successfully",
                data: response.chatId
            }));

            await handleGroupMessage(io, response.chatId, GROUP_CREATE_RESPONSE_EVENT);
        } catch (err) {
            ack(buildErrorResponse(err.message))
        }
    });

    socket.on(GROUP_UPDATE_REQUEST_EVENT, async (reqBody, ack) => {
        try {
            console.info(`Group update request from ${user.name}:`, reqBody);
            const response = await ChatService.updateGroup(reqBody, user);

            ack(ApiResponse({
                message: "Group updated successfully",
                data: response.chatId
            }));

            await handleGroupMessage(io, response.chatId, GROUP_UPDATE_RESPONSE_EVENT);
        } catch (err) {
            ack(buildErrorResponse(err.message))
        }
    });
};

module.exports = setupGroupHandlers;
