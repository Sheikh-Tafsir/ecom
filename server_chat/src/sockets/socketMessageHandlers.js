const MessageService = require('../service/MessageService');
const ChatService = require('../service/ChatService'); // For saveMessageReceipts dependency if needed, or just import what's needed
const ApiResponse = require('../common/ApiResponse');
const {MESSAGE_SEND_EVENT, MESSAGE_RECEIVE_EVENT} = require('./socketEvents');
const {addSocketToRoom, getRoom, getActiveUsersInRoom} = require('./socketRoomManager');
const {getSockets} = require('./socketManager');
const {SENT, RECEIVED} = require("../utils/Messages");
const {buildErrorResponse} = require("../utils/ResponseUtils");

const setupMessageHandlers = (io, socket) => {
    const user = socket.user;

    socket.on(MESSAGE_SEND_EVENT, async (reqBody, ack) => {
        try {
            //console.info(`Message ${reqBody?.content} send request from ${user.name}:`, reqBody);

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

            // Join the room if this is a newly created direct chat
            const roomId = getRoom(message.chatId);
            addSocketToRoom(socket, roomId);

            const senderSockets = getSockets(user.id);
            senderSockets.forEach(senderSocket => addSocketToRoom(senderSocket, roomId));

            if (reqBody.receiverId) {
                const receiverSockets = getSockets(reqBody.receiverId);
                receiverSockets.forEach(receiverSocket => addSocketToRoom(receiverSocket, roomId));
            }

            io.to(roomId).emit(MESSAGE_RECEIVE_EVENT,
                ApiResponse({
                    message: RECEIVED,
                    data: messageData,
                })
            );
            console.info(`Message ${reqBody?.content} sent from ${user.name}:`, messageData);

            const activeUsersInRoom = getActiveUsersInRoom(io, roomId);
            await MessageService.saveMessageReceipts(activeUsersInRoom, message.id, message.chatId, user.id);
        } catch (err) {
            console.error(`Error sending message from ${user.name}:`, err);
            ack(buildErrorResponse(err.message))
        }
    });
};

module.exports = setupMessageHandlers;
