const CacheService = require('./CacheService');
const { logger } = require('../config/logger');

/**
 * PubSubService — Redis Pub/Sub Infrastructure Abstraction
 *
 * Centralizes Redis pub/sub connections, channel subscriptions, and multiplexing,
 * shielding handlers and services from raw Redis client duplication and event loop lifecycle.
 */
class PubSubService {
    constructor() {
        this._subClient = null;
        this._channelHandlers = new Map(); // channelName -> Set<Function>
        this._messageListenerAttached = false;
    }

    /**
     * Get or create the shared Redis subscriber client.
     * @returns {import('ioredis').Redis}
     */
    getSubClient() {
        if (!this._subClient) {
            this._subClient = CacheService.getUnderlyingClient().duplicate();

            this._subClient.on('error', (err) => {
                logger.error('❌ Redis PubSub SubClient error:', err);
            });
        }
        return this._subClient;
    }

    /**
     * Ensure the root 'message' event listener is attached to the subscriber client.
     * @private
     */
    _ensureMessageListener() {
        if (this._messageListenerAttached || !this._subClient) return;

        this._subClient.on('message', async (channel, message) => {
            const handlers = this._channelHandlers.get(channel);
            if (!handlers || handlers.size === 0) return;

            for (const handler of handlers) {
                try {
                    await handler(message, channel);
                } catch (err) {
                    logger.error(`Error in PubSub handler for channel '${channel}':`, err.message);
                }
            }
        });

        this._messageListenerAttached = true;
    }

    /**
     * Subscribe a handler to a Redis channel.
     * @param {string} channel - Channel name
     * @param {Function} handler - Callback taking (message, channel)
     * @returns {import('ioredis').Redis} The underlying subscriber client
     */
    subscribe(channel, handler) {
        if (!channel || typeof handler !== 'function') return null;

        const client = this.getSubClient();
        this._ensureMessageListener();

        if (!this._channelHandlers.has(channel)) {
            this._channelHandlers.set(channel, new Set());

            client.subscribe(channel, (err, count) => {
                if (err) {
                    logger.error(`❌ Failed to subscribe to channel '${channel}':`, err);
                } else {
                    logger.info(`⚡ Subscribed to Redis channel '${channel}'. Active subscriptions: ${count}`);
                }
            });
        }

        this._channelHandlers.get(channel).add(handler);
        return client;
    }

    /**
     * Unsubscribe a handler from a Redis channel.
     * If no handlers remain for the channel, issues an UNSUBSCRIBE command.
     * @param {string} channel
     * @param {Function} [handler] - Optional specific handler; if omitted, removes all handlers for channel
     */
    unsubscribe(channel, handler) {
        if (!this._channelHandlers.has(channel)) return;

        const handlers = this._channelHandlers.get(channel);
        if (handler) {
            handlers.delete(handler);
        } else {
            handlers.clear();
        }

        if (handlers.size === 0) {
            this._channelHandlers.delete(channel);
            if (this._subClient && typeof this._subClient.unsubscribe === 'function') {
                this._subClient.unsubscribe(channel, (err) => {
                    if (err) {
                        logger.error(`Error unsubscribing from channel '${channel}':`, err);
                    }
                });
            }
        }
    }

    /**
     * Publish a message to a Redis channel.
     * @param {string} channel
     * @param {string|object} message
     * @returns {Promise<number>} Number of clients that received the message
     */
    async publish(channel, message) {
        try {
            const client = CacheService.getUnderlyingClient();
            const payload = typeof message === 'object' ? JSON.stringify(message) : String(message);
            return await client.publish(channel, payload);
        } catch (err) {
            logger.error(`Failed to publish message to channel '${channel}':`, err.message);
            return 0;
        }
    }

    /**
     * Close the subscriber connection and clear all handlers.
     */
    async close() {
        if (this._subClient) {
            try {
                if (typeof this._subClient.quit === 'function') {
                    await this._subClient.quit();
                } else if (typeof this._subClient.disconnect === 'function') {
                    this._subClient.disconnect();
                }
                logger.info('Redis PubSub subscriber connection closed.');
            } catch (err) {
                logger.error('Error closing Redis PubSub subscriber:', err);
            } finally {
                this._subClient = null;
                this._messageListenerAttached = false;
                this._channelHandlers.clear();
            }
        }
    }

    /**
     * Create dedicated duplicate Redis clients for the Socket.IO adapter.
     * @returns {{ pubClient: import('ioredis').Redis, subClient: import('ioredis').Redis }}
     */
    createAdapterClients() {
        const underlying = CacheService.getUnderlyingClient();
        const pubClient = underlying.duplicate();
        const subClient = underlying.duplicate();

        pubClient.on('error', (err) => logger.error('❌ Redis Adapter PubClient error:', err));
        subClient.on('error', (err) => logger.error('❌ Redis Adapter SubClient error:', err));

        return { pubClient, subClient };
    }
}

module.exports = new PubSubService();
