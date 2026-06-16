const socketIO = require('socket.io');

const ChatService = require('../service/ChatService');
const ApiResponse = require('../common/ApiResponse');
// const {corsOptions} = require('../middleware/CorsMiddleware');

const {
    MESSAGE_SEND_EVENT,
    MESSAGE_RECEIVE_EVENT,
    GROUP_CREATE_REQUEST_EVENT,
    GROUP_CREATE_RESPONSE_EVENT,
    GROUP_UPDATE_REQUEST_EVENT,
    GROUP_UPDATE_RESPONSE_EVENT,
} = require('./socketEvents');

const {
    addSocket, removeSocket, addUserToRoom,
    removeUserFromAllRooms, getUsersInRoom, getSockets
} = require('./socketManager');
const {ChatParticipant} = require('../model');
const {SENT, RECEIVED, ACCESS_TOKEN_REQUIRED, ACCESS_TOKEN_INVALID} = require("../utils/Messages");
const {buildErrorResponse} = require("../utils/ResponseUtils");
const RuntimeError = require("../common/RuntimeError");
const {isAccessTokenValid} = require("../service/JwtService");
const {findAllChatParticipantsByUserId} = require("../service/ChatService");

const getRoom = (chatId) => `chat_${chatId}`;

module.exports = (server) => {

    const io = socketIO(server)

    /* ---------------- auth middleware ---------------- */

    io.use((socket, next) => {
        try {
            const token = socket.handshake.auth?.token;
            if (!token) {
                console.error("Access token is required for socket connection.");

                const error = new Error(ACCESS_TOKEN_REQUIRED);
                error.data = {
                    status: 401,
                    error: ACCESS_TOKEN_REQUIRED
                };
                
                return next(error);
            }

            socket.user = isAccessTokenValid(token);

            next();
        } catch {
            console.error("Invalid access token.");

            const error = new Error(ACCESS_TOKEN_INVALID);
            error.data = {
                status: 401,
                error: ACCESS_TOKEN_INVALID
            };
                
            return next(error);
        }
    });

    /* ---------------- connection ---------------- */

    io.on('connection', async (socket) => {
        const user = socket.user;

        addSocket(user.id, socket);

        console.info(`Connected: ${user.name}`);

        /* join user rooms */
        const chatParticipants = await findAllChatParticipantsByUserId(user.id);
        chatParticipants.forEach(participants => {
            const roomId = getRoom(participants.chatId);
            socket.join(roomId);
            addUserToRoom(roomId, user.id);
        });


        
        /* ---------------- messaging ---------------- */

        socket.on(MESSAGE_SEND_EVENT, async (reqBody, ack) => {
            try {
                console.info(`Message send request from ${user.name}:`, reqBody);
                const message = await ChatService.sendMessage(user.id, reqBody);

                const messageData = message.toJSON ? message.toJSON() : message;
                if (reqBody.tempId) {
                    messageData.tempId = reqBody.tempId;
                }

                // Join the room if this is a newly created direct chat
                const roomId = getRoom(message.chatId);
                if (!socket.rooms.has(roomId)) {
                    socket.join(roomId);
                    addUserToRoom(roomId, user.id);
                }

                await ChatService.saveMessageReceipts(getUsersInRoom(roomId), message.id, message.chatId, user.id);

                ack(ApiResponse({message: SENT, data: {chatId: message.chatId}}));

                io.to(roomId).emit(MESSAGE_RECEIVE_EVENT,
                    ApiResponse({
                        message: RECEIVED,
                        data: messageData,
                    })
                );
            } catch (err) {
                console.error(`Error sending message from ${user.name}:`, err);
                ack(buildErrorResponse(err.message))
            }
        });

        /* ---------------- group actions ---------------- */

        socket.on(GROUP_CREATE_REQUEST_EVENT, async (reqBody, ack) => {
            try {
                console.info(`Group create request from ${user.name}:`, reqBody);
                const response = await ChatService.createGroup(reqBody, user);

                ack(ApiResponse({
                    message: "Group created successfully",
                    data: response.chatId
                }));

                await handleNewGroupNotification(response.chatId, GROUP_CREATE_RESPONSE_EVENT);
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

                await handleNewGroupNotification(response.chatId, GROUP_UPDATE_RESPONSE_EVENT);
            } catch (err) {
                ack(buildErrorResponse(err.message))
            }
        });

        /* ---------------- disconnect ---------------- */

        socket.on('disconnect', () => {
            removeUserFromAllRooms(user.id);
            removeSocket(user.id, socket.id);
            console.info(`Disconnected: ${user.name}`);
        });
    });

        /* helper to handle group notification */
        const handleNewGroupNotification = async (chatId, eventName) => {
            const roomId = getRoom(chatId);
            const participants = await ChatParticipant.findAll({
                where: {chatId},
                attributes: ['userId'],
                raw: true
            });

            participants.forEach(p => {
                const sockets = getSockets(p.userId);
                sockets.forEach(s => {
                    s.join(roomId);
                    addUserToRoom(roomId, p.userId);
                    s.emit(eventName, ApiResponse({
                        message: "Group event",
                        data: chatId
                    }));
                });
            });
        };
};