const ChatService = require('../service/ChatService'); // Facade for ChatManagementService
const BlockService = require('../service/BlockService');
const ApiResponse = require('../common/ApiResponse');
const {
    GROUP_CREATE_REQUEST_EVENT,
    GROUP_CREATE_RESPONSE_EVENT,
    GROUP_UPDATE_REQUEST_EVENT,
    GROUP_UPDATE_RESPONSE_EVENT
} = require('./socketEvents');
const {handleGroupMessage} = require('./socketRoomManager');
const {buildErrorResponse} = require("../utils/ResponseUtils");
const {logger} = require('../config/logger');

const setupGroupHandlers = (io, socket) => {
    const user = socket.user;

    socket.on(GROUP_CREATE_REQUEST_EVENT, async (reqBody, ack) => {
        try {
            logger.info(`Group create request from ${user.name}:`, reqBody);

            // Block check: creator cannot add users they've blocked (or who blocked them)
            if (reqBody.users?.length) {
                for (const u of reqBody.users) {
                    if (await BlockService.isBlocked(user.id, u.id)) {
                        return ack(buildErrorResponse(`Cannot create group — a blocked user is included.`));
                    }
                }
            }

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
            logger.info(`Group update request from ${user.name}:`, reqBody);

            // Block check: updater cannot add users they've blocked (or who blocked them)
            if (reqBody.users?.length) {
                for (const u of reqBody.users) {
                    if (await BlockService.isBlocked(user.id, u.id)) {
                        return ack(buildErrorResponse(`Cannot add a blocked user to the group.`));
                    }
                }
            }

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
