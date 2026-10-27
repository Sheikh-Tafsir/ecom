const test = require('node:test');
const assert = require('node:assert');
const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../../.env') });
const { EventEmitter } = require('node:events');
const RedisConfig = require('../../src/config/RedisConfig');
const { initUserUpdateSubscriber, closeUserUpdateSubscriber, USER_UPDATES_CHANNEL } = require('../../src/sockets/socketUserUpdateHandler');

test('User Update Subscriber - evicts cache on user-updates message', async (t) => {
    const mockSubClient = new EventEmitter();
    mockSubClient.subscribe = (channel, cb) => {
        if (cb) cb(null, 1);
    };
    mockSubClient.quit = async () => {};

    const originalDuplicate = RedisConfig.duplicate;
    const deletedKeys = [];
    const originalDel = RedisConfig.del;
    RedisConfig.del = async (key) => { deletedKeys.push(key); return 1; };
    RedisConfig.duplicate = () => mockSubClient;

    try {
        initUserUpdateSubscriber();

        const userId = 'abc-123-def';
        mockSubClient.emit('message', USER_UPDATES_CHANNEL, userId);

        // Allow async handler to complete
        await new Promise(resolve => setImmediate(resolve));

        assert.strictEqual(deletedKeys.length, 1, 'Should delete exactly one cache key');
        assert.strictEqual(deletedKeys[0], `cache:user:${userId}`, 'Should delete the correct cache key');
    } finally {
        await closeUserUpdateSubscriber();
        RedisConfig.duplicate = originalDuplicate;
        RedisConfig.del = originalDel;
    }
});

test('User Update Subscriber - ignores messages on other channels', async (t) => {
    const mockSubClient = new EventEmitter();
    mockSubClient.subscribe = (channel, cb) => {
        if (cb) cb(null, 1);
    };
    mockSubClient.quit = async () => {};

    const originalDuplicate = RedisConfig.duplicate;
    const deletedKeys = [];
    const originalDel = RedisConfig.del;
    RedisConfig.del = async (key) => { deletedKeys.push(key); return 1; };
    RedisConfig.duplicate = () => mockSubClient;

    try {
        initUserUpdateSubscriber();

        mockSubClient.emit('message', 'some-other-channel', 'user-id-1');

        await new Promise(resolve => setImmediate(resolve));

        assert.strictEqual(deletedKeys.length, 0, 'Should not delete any keys for unrelated channels');
    } finally {
        await closeUserUpdateSubscriber();
        RedisConfig.duplicate = originalDuplicate;
        RedisConfig.del = originalDel;
    }
});

test('User Update Subscriber - handles Redis del failure gracefully', async (t) => {
    const mockSubClient = new EventEmitter();
    mockSubClient.subscribe = (channel, cb) => {
        if (cb) cb(null, 1);
    };
    mockSubClient.quit = async () => {};

    const originalDuplicate = RedisConfig.duplicate;
    const originalDel = RedisConfig.del;
    RedisConfig.del = async () => { throw new Error('Redis connection refused'); };
    RedisConfig.duplicate = () => mockSubClient;

    try {
        initUserUpdateSubscriber();

        mockSubClient.emit('message', USER_UPDATES_CHANNEL, 'user-fail-123');

        await new Promise(resolve => setImmediate(resolve));

        // Should not throw — test passes if we reach here
        assert.ok(true, 'Should handle Redis failure gracefully without throwing');
    } finally {
        await closeUserUpdateSubscriber();
        RedisConfig.duplicate = originalDuplicate;
        RedisConfig.del = originalDel;
    }
});

test.after(async () => {
    try {
        RedisConfig.disconnect();
    } catch (e) {
        // Ignore
    }
});
