const ChatService = require('../service/ChatService');
const ApiResponse = require('../common/ApiResponse');
const RealtimeService = require('../service/RealtimeService');
const {
    GROUP_CREATE_REQUEST_EVENT,
    GROUP_CREATE_RESPONSE_EVENT,
    GROUP_UPDATE_REQUEST_EVENT,
    GROUP_UPDATE_RESPONSE_EVENT
} = require('./socketEvents');
const { buildErrorResponse } = require("../utils/ResponseUtils");
const { logger } = require('../config/logger');

const setupGroupHandlers = (io, socket) => {
    if (io) {
        RealtimeService.init(io);
    }
    const user = socket.user;

    socket.on(GROUP_CREATE_REQUEST_EVENT, async (reqBody, ack) => {
        try {
            logger.info(`Group create request from ${user.name}:`, reqBody);
            const response = await ChatService.createGroup(reqBody, user);

            if (typeof ack === 'function') {
                ack(ApiResponse({
                    message: "Group created successfully",
                    data: response.chatId
                }));
            }

            await RealtimeService.notifyGroupEvent(response.chatId, GROUP_CREATE_RESPONSE_EVENT);
        } catch (err) {
            if (typeof ack === 'function') {
                ack(buildErrorResponse(err.message));
            }
        }
    });

    socket.on(GROUP_UPDATE_REQUEST_EVENT, async (reqBody, ack) => {
        try {
            logger.info(`Group update request from ${user.name}:`, reqBody);
            const response = await ChatService.updateGroup(reqBody, user);

            if (typeof ack === 'function') {
                ack(ApiResponse({
                    message: "Group updated successfully",
                    data: response.chatId
                }));
            }

            await RealtimeService.notifyGroupEvent(response.chatId, GROUP_UPDATE_RESPONSE_EVENT);
        } catch (err) {
            if (typeof ack === 'function') {
                ack(buildErrorResponse(err.message));
            }
        }
    });
};

module.exports = setupGroupHandlers;
