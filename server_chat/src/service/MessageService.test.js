const { sendMessage } = require('../service/MessageService');
const sequelize = require('../config/SequelizeConfig');
const { ChatParticipant, Chat } = require('../model');

describe('MessageService Authorization', () => {
    let testChat;
    
    beforeAll(async () => {
        // Setup a test chat room
        testChat = await Chat.create({ type: 'group', name: 'Security Test Room' });
        await ChatParticipant.create({ chatId: testChat.id, userId: 1, role: 'member' });
    });

    afterAll(async () => {
        await sequelize.close();
    });

    test('should prevent user from sending message to a room they do not belong to', async () => {
        const maliciousUserId = 999;
        const body = {
            chatId: testChat.id,
            content: 'I should not be able to send this',
            contentType: 'text'
        };

        await expect(sendMessage(maliciousUserId, body)).rejects.toThrow(/Not a participant/);
    });

    test('should allow participant to send message', async () => {
        const validUserId = 1;
        const body = {
            chatId: testChat.id,
            content: 'Hello World',
            contentType: 'text'
        };

        const message = await sendMessage(validUserId, body);
        expect(message.chatId).toBe(testChat.id);
        expect(message.senderId).toBe(validUserId);
    });
});
