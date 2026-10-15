const test = require('node:test');
const assert = require('node:assert');
const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../../.env') });
const { EventEmitter } = require('node:events');
const RedisConfig = require('../config/RedisConfig');
const ChatService = require('../service/ChatService');
const { initNotificationSubscriber, closeNotificationSubscriber } = require('./socketNotificationHandler');
const socketConnectionHandler = require('./socketConnectionHandler');

// Stub ChatService to avoid database connections in unit tests
ChatService.findAllChatParticipantsByUserId = async () => [];

test('Notification Subscriber - USER notification routing', async (t) => {
    // Setup mock subClient
    const mockSubClient = new EventEmitter();
    let subscribedChannel = null;
    mockSubClient.subscribe = (channel, cb) => {
        subscribedChannel = channel;
        if (cb) cb(null, 1);
    };
    mockSubClient.quit = async () => {};

    // Stub RedisConfig.duplicate
    const originalDuplicate = RedisConfig.duplicate;
    RedisConfig.duplicate = () => mockSubClient;

    try {
        const emitted = [];
        const mockIo = {
            to: (room) => ({
                emit: (event, payload) => {
                    emitted.push({ room, event, payload });
                }
            })
        };

        initNotificationSubscriber(mockIo);
        assert.strictEqual(subscribedChannel, 'ecom:notifications');

        // Simulate incoming message for USER
        const userPayload = {
            recipientType: 'USER',
            recipientId: '123e4567-e89b-12d3-a456-426614174000',
            type: 'ORDER_PLACED',
            message: 'Your order has been placed successfully.',
            createdAt: '2026-10-15T10:00:00.000Z'
        };

        mockSubClient.emit('message', 'ecom:notifications', JSON.stringify(userPayload));

        assert.strictEqual(emitted.length, 1);
        assert.strictEqual(emitted[0].room, 'user_123e4567-e89b-12d3-a456-426614174000');
        assert.strictEqual(emitted[0].event, 'notification');
        assert.strictEqual(emitted[0].payload.type, 'ORDER_PLACED');
        assert.strictEqual(emitted[0].payload.message, 'Your order has been placed successfully.');
        assert.strictEqual(emitted[0].payload.createdAt, '2026-10-15T10:00:00.000Z');
    } finally {
        await closeNotificationSubscriber();
        RedisConfig.duplicate = originalDuplicate;
    }
});

test('Notification Subscriber - ADMIN notification routing', async (t) => {
    const mockSubClient = new EventEmitter();
    mockSubClient.subscribe = (channel, cb) => {
        if (cb) cb(null, 1);
    };
    mockSubClient.quit = async () => {};

    const originalDuplicate = RedisConfig.duplicate;
    RedisConfig.duplicate = () => mockSubClient;

    try {
        const emitted = [];
        const mockIo = {
            to: (room) => ({
                emit: (event, payload) => {
                    emitted.push({ room, event, payload });
                }
            })
        };

        initNotificationSubscriber(mockIo);

        // Simulate incoming message for ADMIN
        const adminPayload = {
            recipientType: 'ADMIN',
            type: 'LOW_STOCK',
            message: 'Product SKU-999 is low in stock.',
            createdAt: '2026-10-15T10:05:00.000Z'
        };

        mockSubClient.emit('message', 'ecom:notifications', JSON.stringify(adminPayload));

        assert.strictEqual(emitted.length, 1);
        assert.strictEqual(emitted[0].room, 'admin_room');
        assert.strictEqual(emitted[0].event, 'notification');
        assert.strictEqual(emitted[0].payload.type, 'LOW_STOCK');
        assert.strictEqual(emitted[0].payload.message, 'Product SKU-999 is low in stock.');
        assert.strictEqual(emitted[0].payload.createdAt, '2026-10-15T10:05:00.000Z');
    } finally {
        await closeNotificationSubscriber();
        RedisConfig.duplicate = originalDuplicate;
    }
});

test('Notification Subscriber - Fallback createdAt when not provided', async (t) => {
    const mockSubClient = new EventEmitter();
    mockSubClient.subscribe = (channel, cb) => {
        if (cb) cb(null, 1);
    };
    mockSubClient.quit = async () => {};

    const originalDuplicate = RedisConfig.duplicate;
    RedisConfig.duplicate = () => mockSubClient;

    try {
        const emitted = [];
        const mockIo = {
            to: (room) => ({
                emit: (event, payload) => {
                    emitted.push({ room, event, payload });
                }
            })
        };

        initNotificationSubscriber(mockIo);

        const payloadWithoutCreatedAt = {
            recipientType: 'USER',
            recipientId: 'user-xyz',
            type: 'PAYMENT_SUCCESS',
            message: 'Payment verified'
        };

        mockSubClient.emit('message', 'ecom:notifications', JSON.stringify(payloadWithoutCreatedAt));

        assert.strictEqual(emitted.length, 1);
        assert.strictEqual(emitted[0].room, 'user_user-xyz');
        assert.ok(emitted[0].payload.createdAt);
        assert.strictEqual(typeof emitted[0].payload.createdAt, 'string');
    } finally {
        await closeNotificationSubscriber();
        RedisConfig.duplicate = originalDuplicate;
    }
});

test('Notification Subscriber - Error resilience with invalid JSON', async (t) => {
    const mockSubClient = new EventEmitter();
    mockSubClient.subscribe = (channel, cb) => {
        if (cb) cb(null, 1);
    };
    mockSubClient.quit = async () => {};

    const originalDuplicate = RedisConfig.duplicate;
    RedisConfig.duplicate = () => mockSubClient;

    try {
        const emitted = [];
        const mockIo = {
            to: (room) => ({
                emit: (event, payload) => {
                    emitted.push({ room, event, payload });
                }
            })
        };

        initNotificationSubscriber(mockIo);

        // Emit invalid JSON - should not crash
        mockSubClient.emit('message', 'ecom:notifications', 'not-valid-json{');

        assert.strictEqual(emitted.length, 0);
    } finally {
        await closeNotificationSubscriber();
        RedisConfig.duplicate = originalDuplicate;
    }
});

test('Notification Subscriber - Ignores messages on other channels', async (t) => {
    const mockSubClient = new EventEmitter();
    mockSubClient.subscribe = (channel, cb) => {
        if (cb) cb(null, 1);
    };
    mockSubClient.quit = async () => {};

    const originalDuplicate = RedisConfig.duplicate;
    RedisConfig.duplicate = () => mockSubClient;

    try {
        const emitted = [];
        const mockIo = {
            to: (room) => ({
                emit: (event, payload) => {
                    emitted.push({ room, event, payload });
                }
            })
        };

        initNotificationSubscriber(mockIo);

        mockSubClient.emit('message', 'other:channel', JSON.stringify({ recipientType: 'ADMIN', message: 'test' }));

        assert.strictEqual(emitted.length, 0);
    } finally {
        await closeNotificationSubscriber();
        RedisConfig.duplicate = originalDuplicate;
    }
});

test('Socket Connection Handler - Admin room joining', async (t) => {
    const joinedRooms = [];
    const mockSocket = {
        id: 'socket-123',
        user: {
            id: 'admin-id-1',
            name: 'Admin User',
            permissions: ['user:access', 'admin:access']
        },
        join: (room) => {
            joinedRooms.push(room);
        },
        on: () => {}
    };

    // ChatService mock will throw or return empty participants if DB not connected,
    // so we catch or expect participants query
    try {
        await socketConnectionHandler(null, mockSocket);
    } catch (err) {
        // ChatService DB error is expected in unit test without DB connection
    }

    assert.ok(joinedRooms.includes('user_admin-id-1'), 'Should join personal user room');
    assert.ok(joinedRooms.includes('admin_room'), 'Should join admin_room when admin:access is present');
});

test('Socket Connection Handler - Super Admin room joining', async (t) => {
    const joinedRooms = [];
    const mockSocket = {
        id: 'socket-456',
        user: {
            id: 'super-admin-id-1',
            name: 'Super Admin',
            permissions: ['super_admin:access']
        },
        join: (room) => {
            joinedRooms.push(room);
        },
        on: () => {}
    };

    try {
        await socketConnectionHandler(null, mockSocket);
    } catch (err) {
        // ChatService DB error
    }

    assert.ok(joinedRooms.includes('user_super-admin-id-1'), 'Should join personal user room');
    assert.ok(joinedRooms.includes('admin_room'), 'Should join admin_room when super_admin:access is present');
});

test('Socket Connection Handler - Regular user does not join admin room', async (t) => {
    const joinedRooms = [];
    const mockSocket = {
        id: 'socket-789',
        user: {
            id: 'customer-id-1',
            name: 'Customer',
            permissions: ['user:access']
        },
        join: (room) => {
            joinedRooms.push(room);
        },
        on: () => {}
    };

    try {
        await socketConnectionHandler(null, mockSocket);
    } catch (err) {
        // ChatService DB error
    }

    assert.ok(joinedRooms.includes('user_customer-id-1'), 'Should join personal user room');
    assert.strictEqual(joinedRooms.includes('admin_room'), false, 'Should not join admin_room without admin permissions');
});

test.after(async () => {
    try {
        RedisConfig.disconnect();
    } catch (e) {
        // Ignore
    }
});
