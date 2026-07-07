const MessageService = require('../service/MessageService');
const ChatService = require('../service/ChatService');
const ApiResponse = require('../common/ApiResponse');
const {MESSAGE_SEND_EVENT, MESSAGE_RECEIVE_EVENT} = require('./socketEvents');
const {addSocketToRoom, getRoom, getActiveUsersInRoom} = require('./socketRoomManager');
const {SENT, RECEIVED} = require("../utils/Messages");
const {buildErrorResponse} = require("../utils/ResponseUtils");

const setupMessageHandlers = (io, socket) => {
    const user = socket.user;

    socket.on(MESSAGE_SEND_EVENT, async (reqBody, ack) => {
        try {
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

            // Make all of sender's sockets across all nodes join the room
            io.in(`user_${user.id}`).socketsJoin(roomId);

            if (reqBody.receiverId) {
                // Make all of receiver's sockets across all nodes join the room
                io.in(`user_${reqBody.receiverId}`).socketsJoin(roomId);
            }

            io.to(roomId).emit(MESSAGE_RECEIVE_EVENT,
                ApiResponse({
                    message: RECEIVED,
                    data: messageData,
                })
            );
            console.info(`Message ${reqBody?.content} sent from ${user.name}:`, messageData);

            const activeUsersInRoom = await getActiveUsersInRoom(io, roomId);
            await MessageService.saveMessageReceipts(activeUsersInRoom, message.id, message.chatId, user.id);
        } catch (err) {
            console.error(`Error sending message from ${user.name}:`, err);
            ack(buildErrorResponse(err.message))
        }
    });
};

module.exports = setupMessageHandlers;
