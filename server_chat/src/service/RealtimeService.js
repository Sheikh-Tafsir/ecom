const { logger } = require('../config/logger');
const { chatRoom, userRoom, permRoom, ADMIN_ROOM } = require('../constants/room.constants');
const {
    MESSAGE_RECEIVE_EVENT,
    CHAT_UPDATED_EVENT,
    MESSAGES_SEEN_EVENT,
    USER_BLOCKED_EVENT,
    USER_PRESENCE_EVENT,
} = require('../sockets/socketEvents');
const ApiResponse = require('../common/ApiResponse');
const { RECEIVED } = require('../utils/Messages');

/**
 * RealtimeService — Real-time Event Fan-out & Socket.IO Transport Abstraction
 *
 * Centralizes WebSocket broadcasting, room addressing, and active socket queries,
 * completely shielding controllers and domain services from Socket.IO dependencies.
 */
class RealtimeService {
    constructor() {
        this._io = null;
    }

    /**
     * Bind or update the active Socket.IO server instance.
     * @param {import('socket.io').Server} io
     */
    init(io) {
        this._io = io;
        logger.info('RealtimeService initialized with Socket.IO server instance.');
    }

    /**
     * Get the active Socket.IO server instance.
     * @returns {import('socket.io').Server | null}
     */
    getIo() {
        return this._io;
    }

    /* =========================================================================
     * LOW-LEVEL ROOM EMITTERS
     * ========================================================================= */

    /**
     * Emit an event to a specific chat room.
     * @param {string} chatId - Target chat ID
     * @param {string} event - Event name
     * @param {any} payload - Event data payload
     * @returns {boolean} True if emitted, false if io is unavailable
     */
    emitToChat(chatId, event, payload) {
        if (!this._io) return false;
        this._io.to(chatRoom(chatId)).emit(event, payload);
        return true;
    }

    /**
     * Emit an event to a single user's personal room (all connected devices).
     * @param {string} userId - Target user UUID
     * @param {string} event - Event name
     * @param {any} payload - Event data payload
     * @returns {boolean} True if emitted, false if io is unavailable
     */
    emitToUser(userId, event, payload) {
        if (!this._io) return false;
        this._io.to(userRoom(userId)).emit(event, payload);
        return true;
    }

    /**
     * Emit an event to multiple users' personal rooms.
     * @param {string[]} userIds - Array of target user UUIDs
     * @param {string} event - Event name
     * @param {any} payload - Event data payload
     * @returns {boolean} True if emitted, false if io is unavailable
     */
    emitToUsers(userIds, event, payload) {
        if (!this._io || !Array.isArray(userIds) || userIds.length === 0) return false;
        for (const userId of userIds) {
            this._io.to(userRoom(userId)).emit(event, payload);
        }
        return true;
    }

    /**
     * Emit an event to all users possessing a specific permission.
     * @param {string} permission - Permission name/value
     * @param {string} event - Event name
     * @param {any} payload - Event data payload
     * @returns {boolean} True if emitted, false if io is unavailable
     */
    emitToPermission(permission, event, payload) {
        if (!this._io) return false;
        this._io.to(permRoom(permission)).emit(event, payload);
        return true;
    }

    /**
     * Emit an event to all administrators.
     * @param {string} event - Event name
     * @param {any} payload - Event data payload
     * @returns {boolean} True if emitted, false if io is unavailable
     */
    emitToAdmins(event, payload) {
        if (!this._io) return false;
        this._io.to(ADMIN_ROOM).emit(event, payload);
        return true;
    }

    /**
     * Emit an event to all connected sockets globally.
     * @param {string} event - Event name
     * @param {any} payload - Event data payload
     * @returns {boolean} True if emitted, false if io is unavailable
     */
    emitToAll(event, payload) {
        if (!this._io) return false;
        this._io.emit(event, payload);
        return true;
    }

    /* =========================================================================
     * HIGH-LEVEL DOMAIN BROADCASTS
     * ========================================================================= */

    /**
     * Broadcast newly sent message to active chat room and participant sidebars.
     * @param {string} chatId
     * @param {object} messageData - Serialized message object
     * @param {Array<{userId: string}|string>} participants - Chat participants
     */
    notifyMessageSent(chatId, messageData, participants = []) {
        if (!this._io) return;

        // 1. Broadcast to active chat room subscribers
        this.emitToChat(chatId, MESSAGE_RECEIVE_EVENT, ApiResponse({
            message: RECEIVED,
            data: messageData,
        }));

        // 2. Broadcast sidebar preview updates to participant personal rooms
        const chatUpdate = {
            chatId,
            lastMessage: messageData.content,
            lastSenderId: messageData.senderId,
            lastSent: messageData.createdAt,
            lastSentId: messageData.id,
            contentType: messageData.contentType,
            ...(messageData.tempId ? { tempId: messageData.tempId } : {}),
        };

        const updateResponse = ApiResponse({
            message: RECEIVED,
            data: chatUpdate,
        });

        for (const p of participants) {
            const userId = typeof p === 'object' && p !== null ? p.userId : p;
            if (userId) {
                this.emitToUser(userId, CHAT_UPDATED_EVENT, updateResponse);
            }
        }
    }

    /**
     * Broadcast read receipts / messages seen state to a chat room.
     * @param {string} chatId
     * @param {string} userId
     * @param {string} lastSeen - ISO timestamp
     */
    notifyMessagesSeen(chatId, userId, lastSeen) {
        this.emitToChat(chatId, MESSAGES_SEEN_EVENT, {
            chatId,
            userId,
            lastSeen,
        });
    }

    /**
     * Broadcast block/unblock state updates to both involved parties.
     * @param {string} userId1
     * @param {string} userId2
     * @param {boolean} blocked
     */
    notifyUserBlocked(userId1, userId2, blocked) {
        const payload = { userId: userId1, blocked };
        this.emitToUser(userId1, USER_BLOCKED_EVENT, payload);
        this.emitToUser(userId2, USER_BLOCKED_EVENT, payload);
    }

    /**
     * Broadcast presence changes (online / offline + lastSeen) to target contact IDs.
     * @param {string[]} contactIds
     * @param {string} userId
     * @param {boolean} online
     * @param {string|null} [lastSeen]
     */
    notifyPresence(contactIds, userId, online, lastSeen = null) {
        const payload = {
            userId,
            online,
            ...(lastSeen ? { lastSeen } : {}),
        };
        this.emitToUsers(contactIds, USER_PRESENCE_EVENT, payload);
    }

    /**
     * Broadcast group event (creation, update) to all group participants' personal rooms.
     * @param {string} chatId
     * @param {string} eventName
     */
    async notifyGroupEvent(chatId, eventName) {
        if (!chatId || !eventName) return;
        const { ChatParticipant } = require('../model');
        const participants = await ChatParticipant.findAll({
            where: { chatId },
            attributes: ['userId'],
            raw: true,
        });

        const userIds = participants.map(p => p.userId);
        this.emitToUsers(userIds, eventName, ApiResponse({
            message: "Group event",
            data: chatId,
        }));
    }

    /* =========================================================================
     * SOCKET ROOM MANAGEMENT
     * ========================================================================= */

    /**
     * Join a socket to a chat room.
     * @param {import('socket.io').Socket} socket
     * @param {string} chatId
     */
    joinChatRoom(socket, chatId) {
        if (!socket || !chatId) return;
        socket.join(chatRoom(chatId));
    }

    /**
     * Remove a socket from a chat room.
     * @param {import('socket.io').Socket} socket
     * @param {string} chatId
     */
    leaveChatRoom(socket, chatId) {
        if (!socket || !chatId) return;
        socket.leave(chatRoom(chatId));
    }

    /**
     * Check if a socket has joined a chat room.
     * @param {import('socket.io').Socket} socket
     * @param {string} chatId
     * @returns {boolean}
     */
    isSocketInChatRoom(socket, chatId) {
        if (!socket || !socket.rooms || !chatId) return false;
        return socket.rooms.has(chatRoom(chatId));
    }

    /**
     * Broadcast an event to a chat room excluding the sender socket.
     * @param {import('socket.io').Socket} socket
     * @param {string} chatId
     * @param {string} event
     * @param {any} payload
     */
    broadcastToChatExcept(socket, chatId, event, payload) {
        if (!socket || !chatId) return;
        socket.to(chatRoom(chatId)).emit(event, payload);
    }

    /* =========================================================================
     * CONNECTION & SOCKET QUERIES
     * ========================================================================= */

    /**
     * Fetch distinct user IDs currently connected to this server/cluster.
     * @returns {Promise<string[]>}
     */
    async getConnectedUserIds() {
        if (!this._io) return [];
        try {
            const sockets = await this._io.fetchSockets();
            const userIds = new Set();
            for (const s of sockets) {
                if (s.user?.id) {
                    userIds.add(s.user.id);
                }
            }
            return Array.from(userIds);
        } catch (err) {
            logger.error('RealtimeService: Failed to fetch connected sockets:', err.message);
            return [];
        }
    }
}

module.exports = new RealtimeService();
