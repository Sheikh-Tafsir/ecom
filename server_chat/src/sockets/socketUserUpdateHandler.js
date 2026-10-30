const CacheService = require('../service/CacheService');
const PubSubService = require('../service/PubSubService');
const { logger } = require('../config/logger');
const { USER_UPDATES_CHANNEL, USER_CACHE_PREFIX } = require('../constants/redis.constants');

const handleUserUpdateMessage = async (userId) => {
    if (!userId) return;
    const cacheKey = `${USER_CACHE_PREFIX}${userId}`;
    await CacheService.del(cacheKey);
    logger.info(`Evicted chat user cache for user ${userId}`);
};

/**
 * Subscribes to the `ecom:user-updates` Redis pub/sub channel.
 * When the main server publishes a userId (after update/delete/ban),
 * this handler evicts that user's cached profile so the
 * next chat request fetches fresh data from the API.
 */
const initUserUpdateSubscriber = () => {
    return PubSubService.subscribe(USER_UPDATES_CHANNEL, handleUserUpdateMessage);
};

const closeUserUpdateSubscriber = async () => {
    PubSubService.unsubscribe(USER_UPDATES_CHANNEL, handleUserUpdateMessage);
};

module.exports = {
    initUserUpdateSubscriber,
    closeUserUpdateSubscriber,
    USER_UPDATES_CHANNEL
};
