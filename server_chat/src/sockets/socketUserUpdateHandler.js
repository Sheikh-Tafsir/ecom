const CacheService = require('../service/CacheService');
const { logger } = require('../config/logger');
const { USER_UPDATES_CHANNEL, USER_CACHE_PREFIX } = require('../constants/redis.constants');

let subClient = null;

/**
 * Subscribes to the `ecom:user-updates` Redis pub/sub channel.
 * When the main server publishes a userId (after update/delete/ban),
 * this handler evicts that user's cached profile so the
 * next chat request fetches fresh data from the API.
 */
const initUserUpdateSubscriber = () => {
    if (subClient) {
        return subClient;
    }

    subClient = CacheService.getUnderlyingClient().duplicate();

    subClient.on('error', (err) => {
        logger.error('❌ Redis User-Update SubClient error:', err);
    });

    subClient.subscribe(USER_UPDATES_CHANNEL, (err, count) => {
        if (err) {
            logger.error(`❌ Failed to subscribe to channel '${USER_UPDATES_CHANNEL}':`, err);
        } else {
            logger.info(`⚡ Subscribed to Redis channel '${USER_UPDATES_CHANNEL}'. Total active subscriptions: ${count}`);
        }
    });

    subClient.on('message', async (channel, userId) => {
        if (channel !== USER_UPDATES_CHANNEL || !userId) {
            return;
        }

        const cacheKey = `${USER_CACHE_PREFIX}${userId}`;
        await CacheService.del(cacheKey);
        logger.info(`Evicted chat user cache for user ${userId}`);
    });

    return subClient;
};

const closeUserUpdateSubscriber = async () => {
    if (subClient) {
        try {
            await subClient.quit();
            logger.info('Redis user-update subscriber connection closed.');
        } catch (err) {
            logger.error('Error closing Redis user-update subscriber:', err);
        } finally {
            subClient = null;
        }
    }
};

process.on('exit', () => {
    if (subClient) {
        try {
            subClient.disconnect();
        } catch (err) {
            // Ignore during process termination
        }
    }
});

module.exports = {
    initUserUpdateSubscriber,
    closeUserUpdateSubscriber,
    USER_UPDATES_CHANNEL
};
