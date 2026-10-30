const { describe, it, beforeEach, afterEach, after } = require('node:test');
const assert = require('node:assert');
const { EventEmitter } = require('node:events');
const RedisConfig = require('../../src/config/RedisConfig');
const CacheService = require('../../src/service/CacheService');
const PubSubService = require('../../src/service/PubSubService');

describe('PubSubService Unit Tests', () => {
    let mockSubClient;
    let mockUnderlyingClient;
    let originalGetUnderlyingClient;

    beforeEach(() => {
        mockSubClient = new EventEmitter();
        mockSubClient.subscribedChannels = [];
        mockSubClient.unsubscribedChannels = [];
        mockSubClient.subscribe = (channel, cb) => {
            mockSubClient.subscribedChannels.push(channel);
            if (cb) cb(null, 1);
        };
        mockSubClient.unsubscribe = (channel, cb) => {
            mockSubClient.unsubscribedChannels.push(channel);
            if (cb) cb(null, 0);
        };
        mockSubClient.quit = async () => {};

        mockUnderlyingClient = {
            duplicate: () => mockSubClient,
            publish: async (channel, message) => 1,
        };

        originalGetUnderlyingClient = CacheService.getUnderlyingClient;
        CacheService.getUnderlyingClient = () => mockUnderlyingClient;
    });

    afterEach(async () => {
        await PubSubService.close();
        CacheService.getUnderlyingClient = originalGetUnderlyingClient;
    });

    after(() => {
        try {
            RedisConfig.disconnect();
        } catch (e) {}
    });

    it('should subscribe to a channel and invoke callback when message is received', async () => {
        const received = [];
        PubSubService.subscribe('test:channel', (msg, ch) => {
            received.push({ msg, ch });
        });

        assert.ok(mockSubClient.subscribedChannels.includes('test:channel'));

        mockSubClient.emit('message', 'test:channel', 'hello world');
        await new Promise(resolve => setImmediate(resolve));

        assert.strictEqual(received.length, 1);
        assert.strictEqual(received[0].msg, 'hello world');
        assert.strictEqual(received[0].ch, 'test:channel');
    });

    it('should support multiple handlers for the same channel', async () => {
        let count1 = 0;
        let count2 = 0;

        PubSubService.subscribe('multi:channel', () => { count1++; });
        PubSubService.subscribe('multi:channel', () => { count2++; });

        mockSubClient.emit('message', 'multi:channel', 'ping');
        await new Promise(resolve => setImmediate(resolve));

        assert.strictEqual(count1, 1);
        assert.strictEqual(count2, 1);
    });

    it('should unsubscribe a specific handler and issue unsubscribe command when empty', async () => {
        const received = [];
        const handler = (msg) => received.push(msg);

        PubSubService.subscribe('unsub:channel', handler);
        mockSubClient.emit('message', 'unsub:channel', 'first');
        await new Promise(resolve => setImmediate(resolve));
        assert.strictEqual(received.length, 1);

        PubSubService.unsubscribe('unsub:channel', handler);
        assert.ok(mockSubClient.unsubscribedChannels.includes('unsub:channel'));

        mockSubClient.emit('message', 'unsub:channel', 'second');
        await new Promise(resolve => setImmediate(resolve));
        assert.strictEqual(received.length, 1);
    });

    it('should catch errors thrown by handler callbacks without crashing', async () => {
        let secondCalled = false;
        PubSubService.subscribe('err:channel', () => {
            throw new Error('Explosion');
        });
        PubSubService.subscribe('err:channel', () => {
            secondCalled = true;
        });

        mockSubClient.emit('message', 'err:channel', 'boom');
        await new Promise(resolve => setImmediate(resolve));

        assert.strictEqual(secondCalled, true);
    });

    it('should publish message using underlying client', async () => {
        let publishedChannel = null;
        let publishedMessage = null;
        mockUnderlyingClient.publish = async (ch, msg) => {
            publishedChannel = ch;
            publishedMessage = msg;
            return 1;
        };

        const count = await PubSubService.publish('events:pub', { foo: 'bar' });
        assert.strictEqual(count, 1);
        assert.strictEqual(publishedChannel, 'events:pub');
        assert.strictEqual(publishedMessage, '{"foo":"bar"}');
    });

    it('should create adapter clients with duplicate and error handlers', () => {
        const clients = PubSubService.createAdapterClients();
        assert.ok(clients.pubClient);
        assert.ok(clients.subClient);
        try {
            clients.pubClient.disconnect();
            clients.subClient.disconnect();
        } catch (e) {}
    });
});
