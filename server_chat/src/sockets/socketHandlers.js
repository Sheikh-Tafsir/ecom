const socketIO = require('socket.io');

const ChatService = require('../service/ChatService');
const ApiResponse = require('../common/ApiResponse');
// const {corsOptions} = require('../middleware/CorsMiddleware');

const {
    MESSAGE_SEND_EVENT,
    MESSAGE_RECEIVE_EVENT,
} = require('./socketEvents');

const {
    addSocket,
    removeSocket,
} = require('./socketManager');
const {SENT, RECEIVED, ACCESS_TOKEN_REQUIRED, ACCESS_TOKEN_INVALID} = require("../utils/Messages");
const {buildErrorResponse} = require("../utils/ResponseUtils");
const RuntimeError = require("../common/RuntimeError");
const {isAccessTokenValid} = require("../service/JwtService");
const {findAllChatsByUserIdToJoinRoom} = require("../service/ChatService");

const getRoom = (chatId) => `chat_${chatId}`;

module.exports = (server) => {

    const io = socketIO(server)

    /* ---------------- auth middleware ---------------- */

    io.use((socket, next) => {
        try {
            const token = socket.handshake.auth?.token;
            if (!token) {
                console.error("Access token is required for socket connection.");
                return next(new RuntimeError(401, ACCESS_TOKEN_REQUIRED));
            }

            socket.user = isAccessTokenValid(token);

            next();
        } catch {
            console.error("Invalid access token.");
            return next(new RuntimeError(401, ACCESS_TOKEN_INVALID));
        }
    });

    /* ---------------- connection ---------------- */

    io.on('connection', async (socket) => {
        const user = socket.user;

        addSocket(user.id, socket);

        console.log(`Connected: ${user.name}`);

        /* join user rooms */
        const chatParticipants = await findAllChatsByUserIdToJoinRoom(user.id);
        chatParticipants.forEach(participants => socket.join(getRoom(participants.chatId)));

        /* ---------------- messaging ---------------- */

        socket.on(MESSAGE_SEND_EVENT, async (reqBody, ack) => {
            try {
                const response = await ChatService.sendMessage(user.id, reqBody);

                ack(ApiResponse({message: SENT}));

                io.to(getRoom(response?.chatId)).emit(MESSAGE_RECEIVE_EVENT,
                    ApiResponse({
                        message: RECEIVED,
                        data: response,
                    })
                );
            } catch (err) {
                ack(buildErrorResponse(err.message))
            }
        });

        /* ---------------- disconnect ---------------- */

        socket.on('disconnect', () => {
            removeSocket(user.id, socket.id);
            console.log(`Disconnected: ${user.name}`);
        });
    });
};