const { describe, it, before, after } = require('node:test');
const assert = require('node:assert');
const { sendMessage } = require('./MessageService');
const sequelize = require('../config/SequelizeConfig');
const { ChatParticipant, Chat, Message, MessageReceipt } = require('../model');

describe('MessageService Authorization', () => {
    let testChat;
    let isDbConnected = false;
    const testChatId = '018f4a3e-62b1-7890-a234-56789abcdef0';
    const validUserId = '018f4a3e-62b1-7890-a234-56789abcdef1';
    const maliciousUserId = '018f4a3e-62b1-7890-a234-56789abcdef2';

    before(async () => {
        try {
            await sequelize.authenticate();
            isDbConnected = true;
            testChat = await Chat.create({ type: 'group', name: 'Security Test Room' });
            await ChatParticipant.create({ chatId: testChat.id, userId: validUserId, role: 'member' });
        } catch (e) {
            isDbConnected = false;
            // Stub for unit testing when DB is unavailable
            testChat = { id: testChatId, type: 'group', name: 'Security Test Room' };

            const mockTransaction = {
                commit: async () => {},
                rollback: async () => {}
            };
            sequelize.transaction = async () => mockTransaction;

            ChatParticipant.findOne = async ({ where }) => {
                if (where.chatId === testChat.id && where.userId === validUserId) {
                    return { role: 'member' };
                }
                return null;
            };

            Chat.findByPk = async (id) => {
                if (id === testChat.id) {
                    return {
                        id: testChat.id,
                        update: async () => {}
                    };
                }
                return null;
            };

            Message.create = async (data) => data;
            ChatParticipant.increment = async () => [1];
            ChatParticipant.findAll = async () => [{ userId: validUserId }];
            MessageReceipt.bulkCreate = async (receipts) => receipts;
        }
    });

    after(async () => {
        try {
            if (isDbConnected && testChat) {
                await ChatParticipant.destroy({ where: { chatId: testChat.id } });
                await Chat.destroy({ where: { id: testChat.id } });
            }
        } catch (e) {
            // Ignore cleanup errors
        }
        try {
            await sequelize.close();
        } catch (e) {}
        try {
            const RedisConfig = require('../config/RedisConfig');
            RedisConfig.disconnect();
        } catch (e) {}
    });

    it('should prevent user from sending message to a room they do not belong to', async () => {
        const body = {
            chatId: testChat.id,
            content: 'I should not be able to send this',
            contentType: 'text'
        };

        await assert.rejects(
            async () => sendMessage(maliciousUserId, body),
            /Not a participant/
        );
    });

    it('should allow participant to send message', async () => {
        const body = {
            chatId: testChat.id,
            content: 'Hello World',
            contentType: 'text'
        };

        const message = await sendMessage(validUserId, body);
        assert.strictEqual(message.chatId, testChat.id);
        assert.strictEqual(message.senderId, validUserId);
    });
});
