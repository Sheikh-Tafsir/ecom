const { describe, it, beforeEach } = require('node:test');
const assert = require('node:assert');
const RealtimeService = require('../../src/service/RealtimeService');
const {
    MESSAGE_RECEIVE_EVENT,
    CHAT_UPDATED_EVENT,
    MESSAGES_SEEN_EVENT,
    USER_BLOCKED_EVENT,
    USER_PRESENCE_EVENT,
} = require('../../src/sockets/socketEvents');

describe('RealtimeService Unit Tests', () => {
    let mockIo;
    let emittedEvents;

    beforeEach(() => {
        emittedEvents = [];
        mockIo = {
            to: (room) => ({
                emit: (event, payload) => {
                    emittedEvents.push({ target: room, event, payload });
                }
            }),
            emit: (event, payload) => {
                emittedEvents.push({ target: 'GLOBAL', event, payload });
            },
            fetchSockets: async () => [
                { id: 's1', user: { id: 'user-1' } },
                { id: 's2', user: { id: 'user-2' } },
                { id: 's3', user: { id: 'user-1' } }, // multi-tab duplicate
                { id: 's4', user: null }, // unauthenticated/anonymous
            ]
        };
        RealtimeService.init(mockIo);
    });

    it('should return false safely when io is not initialized', () => {
        const uninitializedService = new (RealtimeService.constructor)();
        assert.strictEqual(uninitializedService.emitToChat('chat-1', 'test', {}), false);
        assert.strictEqual(uninitializedService.emitToUser('user-1', 'test', {}), false);
        assert.strictEqual(uninitializedService.emitToUsers(['u1'], 'test', {}), false);
        assert.strictEqual(uninitializedService.emitToPermission('admin', 'test', {}), false);
        assert.strictEqual(uninitializedService.emitToAdmins('test', {}), false);
        assert.strictEqual(uninitializedService.emitToAll('test', {}), false);
    });

    it('should emit to chat room', () => {
        const payload = { msg: 'hello' };
        const result = RealtimeService.emitToChat('chat-123', 'custom-event', payload);
        assert.strictEqual(result, true);
        assert.strictEqual(emittedEvents.length, 1);
        assert.strictEqual(emittedEvents[0].target, 'chat_chat-123');
        assert.strictEqual(emittedEvents[0].event, 'custom-event');
        assert.deepStrictEqual(emittedEvents[0].payload, payload);
    });

    it('should emit to user personal room', () => {
        const payload = { msg: 'alert' };
        const result = RealtimeService.emitToUser('user-456', 'user-event', payload);
        assert.strictEqual(result, true);
        assert.strictEqual(emittedEvents.length, 1);
        assert.strictEqual(emittedEvents[0].target, 'user_user-456');
        assert.strictEqual(emittedEvents[0].event, 'user-event');
        assert.deepStrictEqual(emittedEvents[0].payload, payload);
    });

    it('should emit to multiple user personal rooms', () => {
        const payload = { msg: 'broadcast' };
        const result = RealtimeService.emitToUsers(['user-1', 'user-2'], 'bulk-event', payload);
        assert.strictEqual(result, true);
        assert.strictEqual(emittedEvents.length, 2);
        assert.strictEqual(emittedEvents[0].target, 'user_user-1');
        assert.strictEqual(emittedEvents[1].target, 'user_user-2');
    });

    it('should emit to permission and admin rooms', () => {
        RealtimeService.emitToPermission('product:create', 'notif', { a: 1 });
        RealtimeService.emitToAdmins('notif', { b: 2 });
        RealtimeService.emitToAll('global-event', { c: 3 });

        assert.strictEqual(emittedEvents.length, 3);
        assert.strictEqual(emittedEvents[0].target, 'perm_product:create');
        assert.strictEqual(emittedEvents[1].target, 'admin_room');
        assert.strictEqual(emittedEvents[2].target, 'GLOBAL');
    });

    it('should broadcast notifyMessageSent to chat room and participant personal rooms', () => {
        const messageData = {
            id: 'msg-1',
            chatId: 'chat-999',
            senderId: 'user-sender',
            content: 'Hello everyone!',
            contentType: 'text',
            createdAt: '2026-10-30T12:00:00Z',
            tempId: 'temp-123',
        };
        const participants = [
            { userId: 'user-sender' },
            { userId: 'user-recipient' }
        ];

        RealtimeService.notifyMessageSent('chat-999', messageData, participants);

        assert.strictEqual(emittedEvents.length, 3);
        // 1. Chat room
        assert.strictEqual(emittedEvents[0].target, 'chat_chat-999');
        assert.strictEqual(emittedEvents[0].event, MESSAGE_RECEIVE_EVENT);
        assert.strictEqual(emittedEvents[0].payload.data.id, 'msg-1');

        // 2. Participant sidebars
        assert.strictEqual(emittedEvents[1].target, 'user_user-sender');
        assert.strictEqual(emittedEvents[1].event, CHAT_UPDATED_EVENT);
        assert.strictEqual(emittedEvents[1].payload.data.lastMessage, 'Hello everyone!');
        assert.strictEqual(emittedEvents[1].payload.data.tempId, 'temp-123');

        assert.strictEqual(emittedEvents[2].target, 'user_user-recipient');
        assert.strictEqual(emittedEvents[2].event, CHAT_UPDATED_EVENT);
    });

    it('should broadcast notifyMessagesSeen', () => {
        RealtimeService.notifyMessagesSeen('chat-1', 'user-1', '2026-10-30T12:00:00Z');
        assert.strictEqual(emittedEvents.length, 1);
        assert.strictEqual(emittedEvents[0].target, 'chat_chat-1');
        assert.strictEqual(emittedEvents[0].event, MESSAGES_SEEN_EVENT);
        assert.deepStrictEqual(emittedEvents[0].payload, {
            chatId: 'chat-1',
            userId: 'user-1',
            lastSeen: '2026-10-30T12:00:00Z',
        });
    });

    it('should broadcast notifyUserBlocked to both users', () => {
        RealtimeService.notifyUserBlocked('user-a', 'user-b', true);
        assert.strictEqual(emittedEvents.length, 2);
        assert.strictEqual(emittedEvents[0].target, 'user_user-a');
        assert.strictEqual(emittedEvents[0].event, USER_BLOCKED_EVENT);
        assert.deepStrictEqual(emittedEvents[0].payload, { userId: 'user-a', blocked: true });
        assert.strictEqual(emittedEvents[1].target, 'user_user-b');
    });

    it('should broadcast notifyPresence to target contacts', () => {
        RealtimeService.notifyPresence(['contact-1', 'contact-2'], 'user-1', false, '2026-10-30T12:05:00Z');
        assert.strictEqual(emittedEvents.length, 2);
        assert.strictEqual(emittedEvents[0].target, 'user_contact-1');
        assert.strictEqual(emittedEvents[0].event, USER_PRESENCE_EVENT);
        assert.deepStrictEqual(emittedEvents[0].payload, {
            userId: 'user-1',
            online: false,
            lastSeen: '2026-10-30T12:05:00Z',
        });
    });

    it('should extract distinct connected user IDs ignoring anonymous and duplicates', async () => {
        const userIds = await RealtimeService.getConnectedUserIds();
        assert.deepStrictEqual(userIds, ['user-1', 'user-2']);
    });

    it('should manage socket room joining, leaving, and check membership', () => {
        const joinedRooms = new Set();
        const mockSocket = {
            rooms: joinedRooms,
            join: (r) => joinedRooms.add(r),
            leave: (r) => joinedRooms.delete(r),
            to: (room) => ({
                emit: (event, payload) => {
                    emittedEvents.push({ target: room, event, payload });
                }
            })
        };

        RealtimeService.joinChatRoom(mockSocket, 'chat-abc');
        assert.ok(mockSocket.rooms.has('chat_chat-abc'));
        assert.strictEqual(RealtimeService.isSocketInChatRoom(mockSocket, 'chat-abc'), true);

        RealtimeService.broadcastToChatExcept(mockSocket, 'chat-abc', 'typing', { user: 'me' });
        assert.strictEqual(emittedEvents.length, 1);
        assert.strictEqual(emittedEvents[0].target, 'chat_chat-abc');
        assert.strictEqual(emittedEvents[0].event, 'typing');

        RealtimeService.leaveChatRoom(mockSocket, 'chat-abc');
        assert.strictEqual(RealtimeService.isSocketInChatRoom(mockSocket, 'chat-abc'), false);
    });
});
