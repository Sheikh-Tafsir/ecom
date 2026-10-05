const MessageService = require('../service/MessageService');
const ChatService = require('../service/ChatService');
const ApiResponse = require('../common/ApiResponse');
const {MESSAGE_SEND_EVENT, MESSAGE_RECEIVE_EVENT} = require('./socketEvents');
const {addSocketToRoom, getRoom, getActiveUsersInRoom} = require('./socketRoomManager');
const {SENT, RECEIVED} = require("../utils/Messages");
const {buildErrorResponse} = require("../utils/ResponseUtils");
const RedisConfig = require('../config/RedisConfig');

const MAX_MESSAGE_LENGTH = 4096;

// Per-user Redis-backed sliding window rate limiter: max 20 messages per 10 seconds
// Key: ratelimit:msg:<userId> — stored as a Redis counter with a 10-second TTL window
const MESSAGE_RATE_LIMIT = 20;
const MESSAGE_RATE_WINDOW_SEC = 10;

// Fallback: in-memory per-socket counter used when Redis is unavailable
const FALLBACK_RATE_LIMIT = 5;
const FALLBACK_RATE_WINDOW_MS = 1000;

async function isRateLimitedRedis(userId) {
    const key = `ratelimit:msg:${userId}`;
    const count = await RedisConfig.incr(key);
    if (count === 1) {
        // First message in this window — set expiry
        await RedisConfig.expire(key, MESSAGE_RATE_WINDOW_SEC);
    }
    return count > MESSAGE_RATE_LIMIT;
}

const setupMessageHandlers = (io, socket) => {
    const user = socket.user;

    // Fallback per-socket counter (used only when Redis is unavailable)
    let fallbackCount = 0;
    let fallbackReset = Date.now();

    socket.on(MESSAGE_SEND_EVENT, async (reqBody, ack) => {
        try {
            // --- Rate limiting (Redis-backed, per-user across all sockets) ---
            let limited = false;
            try {
                limited = await isRateLimitedRedis(user.id);
            } catch (redisErr) {
                // Redis unavailable — fall back to per-socket counter
                console.warn('Redis rate-limit unavailable, using fallback:', redisErr.message);
                const now = Date.now();
                if (now - fallbackReset > FALLBACK_RATE_WINDOW_MS) {
                    fallbackCount = 0;
                    fallbackReset = now;
                }
                if (fallbackCount >= FALLBACK_RATE_LIMIT) {
                    limited = true;
                } else {
                    fallbackCount++;
                }
            }

            if (limited) {
                socket.emit('error', { message: 'Rate limit exceeded. Slow down.' });
                return ack(buildErrorResponse("Rate limit exceeded. Slow down."));
            }

            // --- Content length validation (M-7) ---
            const content = reqBody?.content;
            if (!content || content.length > MAX_MESSAGE_LENGTH) {
                socket.emit('error', { message: 'Message too long or empty.' });
                return ack(buildErrorResponse("Message too long or empty."));
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

            // Join all participants of this chat to the room so they receive the message
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
            // console.info("Message %s sent from %s:", reqBody?.content, user.name, messageData);

            const activeUsersInRoom = await getActiveUsersInRoom(io, roomId);
            await MessageService.saveMessageReceipts(activeUsersInRoom, message.id, message.chatId, user.id);
        } catch (err) {
            console.error(`Error sending message from ${user.name}:`, err);
            ack(buildErrorResponse(err.message))
        }
    });
};

module.exports = setupMessageHandlers;
