const { describe, it, before, after } = require('node:test');
const assert = require('node:assert');
const ChatService = require('../../src/service/ChatService');
const UserService = require('../../src/service/UserService');
const { Chat, ChatParticipant, Message } = require('../../src/model');
const RedisConfig = require('../../src/config/RedisConfig');
const sequelize = require('../../src/config/SequelizeConfig');

describe('ChatService Unit Tests (Hydration & Decoupling)', () => {
    let originalChatParticipantFindAll;
    let originalChatParticipantFindOne;
    let originalChatFindAll;
    let originalChatFindByPk;
    let originalMessageFindAll;
    let originalGetUsersBatch;

    before(() => {
        originalChatParticipantFindAll = ChatParticipant.findAll;
        originalChatParticipantFindOne = ChatParticipant.findOne;
        originalChatFindAll = Chat.findAll;
        originalChatFindByPk = Chat.findByPk;
        originalMessageFindAll = Message.findAll;
        originalGetUsersBatch = UserService.getUsersBatch;
    });

    after(async () => {
        ChatParticipant.findAll = originalChatParticipantFindAll;
        ChatParticipant.findOne = originalChatParticipantFindOne;
        Chat.findAll = originalChatFindAll;
        Chat.findByPk = originalChatFindByPk;
        Message.findAll = originalMessageFindAll;
        UserService.getUsersBatch = originalGetUsersBatch;
        try {
            await sequelize.close();
        } catch (e) {}
        try {
            RedisConfig.disconnect();
        } catch (e) {}
    });

    it('findAllChatsByUserId hydrates participants from UserService without User model', async () => {
        const userId = 'user-me';
        const otherUserId = 'user-other';
        const chatId = 'chat-1';

        ChatParticipant.findAll = async () => [{ chatId }];

        Chat.findAll = async (options) => {
            // Verify that User model is NOT included
            const include = options.include || [];
            const participantInclude = include.find(i => i.as === 'Participants');
            assert.ok(participantInclude, 'Participants should be included');
            assert.strictEqual(participantInclude.include, undefined, 'User model must NOT be nested in Participants');

            return [
                {
                    id: chatId,
                    type: 'direct',
                    name: null,
                    image: null,
                    lastMessage: 'Hi there',
                    lastSent: new Date('2026-10-15T12:00:00Z'),
                    lastSenderId: otherUserId,
                    Participants: [
                        { userId: 'user-me', role: 'member', unreadMessage: 0 },
                        { userId: 'user-other', role: 'member', unreadMessage: 0 }
                    ],
                    get: function() { return this; }
                }
            ];
        };

        UserService.getUsersBatch = async (userIds, token) => {
            assert.ok(userIds.includes('user-me'));
            assert.ok(userIds.includes('user-other'));
            return {
                'user-me': { id: 'user-me', name: 'My Name', image: null },
                'user-other': { id: 'user-other', name: 'Other User', image: 'https://example.com/other.png' }
            };
        };

        const result = await ChatService.findAllChatsByUserId({}, userId, 'Bearer test-token');
        assert.strictEqual(result.chats.length, 1);
        const chat = result.chats[0];
        assert.strictEqual(chat.id, chatId);
        assert.strictEqual(chat.name, 'Other User');
        assert.strictEqual(chat.image, 'https://example.com/other.png');
        assert.strictEqual(chat.otherUserId, 'user-other');
        assert.strictEqual(chat.shortName, 'Other');

        const participants = chat.participants;
        assert.strictEqual(participants.length, 2);
        const otherP = participants.find(p => p.userId === 'user-other');
        assert.strictEqual(otherP.name, 'Other User');
        assert.strictEqual(otherP.image, 'https://example.com/other.png');
    });

    it('findDetailsChatById hydrates participants from UserService without User model', async () => {
        const userId = 'user-me';
        const otherUserId = 'user-other';
        const chatId = 'chat-2';

        ChatParticipant.findOne = async () => ({ role: 'member' });

        Chat.findByPk = async (id, options) => {
            const include = options.include || [];
            const participantInclude = include.find(i => i.as === 'Participants');
            assert.ok(participantInclude, 'Participants should be included');
            assert.strictEqual(participantInclude.include, undefined, 'User model must NOT be nested in Participants');

            return {
                id: chatId,
                type: 'direct',
                name: null,
                Participants: [
                    { userId: 'user-me', role: 'member' },
                    { userId: 'user-other', role: 'member' }
                ],
                get: function() { return this; }
            };
        };

        Message.findAll = async () => [
            { id: 'msg-1', chatId, senderId: otherUserId, content: 'Hello' }
        ];

        UserService.getUsersBatch = async (userIds, token) => {
            return {
                'user-me': { id: 'user-me', name: 'My Name', image: null },
                'user-other': { id: 'user-other', name: 'Other User', image: 'https://example.com/other.png' }
            };
        };

        const result = await ChatService.findDetailsChatById(chatId, {}, userId, 'Bearer test-token');
        assert.strictEqual(result.id, chatId);
        assert.strictEqual(result.name, 'Other User');
        assert.strictEqual(result.messages.length, 1);
        assert.strictEqual(result.messages[0].content, 'Hello');

        const otherP = result.participants.find(p => p.userId === 'user-other');
        assert.strictEqual(otherP.name, 'Other User');
        assert.strictEqual(otherP.image, 'https://example.com/other.png');
    });
});
