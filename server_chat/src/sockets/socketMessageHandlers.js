const MessageService = require('../service/MessageService');
const ChatService = require('../service/ChatService');
const ApiResponse = require('../common/ApiResponse');
const {MESSAGE_SEND_EVENT, MESSAGE_RECEIVE_EVENT} = require('./socketEvents');
const {addSocketToRoom, getRoom, getActiveUsersInRoom} = require('./socketRoomManager');
const {SENT, RECEIVED} = require("../utils/Messages");
const {buildErrorResponse} = require("../utils/ResponseUtils");

const setupMessageHandlers = (io, socket) => {
    const user = socket.user;
    
    // Simple rate limiting: 5 messages per second
    let messageCount = 0;
    let lastReset = Date.now();

    socket.on(MESSAGE_SEND_EVENT, async (reqBody, ack) => {
        try {
            const now = Date.now();
            if (now - lastReset > 1000) {
                messageCount = 0;
                lastReset = now;
            }

            if (messageCount >= 5) {
                return ack(buildErrorResponse("Rate limit exceeded. Please wait a moment."));
            }

            messageCount++;

            if (!reqBody?.content) {
                ack(buildErrorResponse("Message is required"))
                return;
            }

            const message = await MessageService.sendMessage(user.id, reqBody);

            ack(ApiResponse({
                message: SENT,
                data: {chatId: message.chatId}
            }));

            const messageData = message.toJSON ? message.toJSON() : message;
            if (reqBody.tempId) {
                messageData.tempId = reqBody.tempId;
            }

            const roomId = getRoom(message.chatId);
            addSocketToRoom(socket, roomId);

            // Securely join all participants of this chat to the room
            const participants = await ChatService.findChatParticipantsByChatId(message.chatId);
            participants.forEach(p => {
                io.in(`user_${p.userId}`).socketsJoin(roomId);
            });

            io.to(roomId).emit(MESSAGE_RECEIVE_EVENT,
                ApiResponse({
                    message: RECEIVED,
                    data: messageData,
                })
            );
            console.info("Message %s sent from %s:", reqBody?.content, user.name, messageData);

            const activeUsersInRoom = await getActiveUsersInRoom(io, roomId);
            await MessageService.saveMessageReceipts(activeUsersInRoom, message.id, message.chatId, user.id);
        } catch (err) {
            console.error(`Error sending message from ${user.name}:`, err);
            ack(buildErrorResponse(err.message))
        }
    });
};

module.exports = setupMessageHandlers;
