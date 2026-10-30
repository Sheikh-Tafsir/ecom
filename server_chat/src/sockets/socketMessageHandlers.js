const MessageService = require('../service/MessageService');
const ApiResponse = require('../common/ApiResponse');
const { MESSAGE_SEND_EVENT } = require('./socketEvents');
const { SENT } = require('../utils/Messages');
const { buildErrorResponse } = require('../utils/ResponseUtils');
const { logger } = require('../config/logger');
const TrimInput = require('../middleware/TrimInput');

/**
 * Socket.IO Message Handler (Legacy / Ingress Fallback)
 *
 * Delegates message validation, rate limiting, persistence, and fan-out
 * to MessageService.processAndSendMessage to ensure 100% parity with HTTP API.
 */
const setupMessageHandlers = (io, socket) => {
    const user = socket.user;

    socket.on(MESSAGE_SEND_EVENT, async (reqBody, ack) => {
        try {
            TrimInput.sanitize(reqBody);

            const messageData = await MessageService.processAndSendMessage(user, reqBody);

            if (typeof ack === 'function') {
                ack(ApiResponse({
                    message: SENT,
                    data: { chatId: messageData.chatId, message: messageData },
                }));
            }
        } catch (err) {
            logger.error(`Error in WebSocket message send from user ${user?.id}:`, err.message);
            if (typeof ack === 'function') {
                ack(buildErrorResponse(err.message));
            }
        }
    });
};

module.exports = setupMessageHandlers;
