const RedisConfig = require('../config/RedisConfig');
const { logger } = require('../config/logger');
const { NOTIFICATION_EVENT } = require('./socketEvents');
const { NOTIFICATION_CHANNEL } = require('../constants/redis.constants');
const { userRoom, permRoom, ADMIN_ROOM } = require('../constants/room.constants');

let subClient = null;

const initNotificationSubscriber = (io) => {
    if (subClient) {
        return subClient;
    }

    subClient = RedisConfig.duplicate();

    subClient.on('error', (err) => {
        logger.error('❌ Redis Notification SubClient error:', err);
    });

    subClient.subscribe(NOTIFICATION_CHANNEL, (err, count) => {
        if (err) {
            logger.error(`❌ Failed to subscribe to channel '${NOTIFICATION_CHANNEL}':`, err);
        } else {
            logger.info(`⚡ Subscribed to Redis channel '${NOTIFICATION_CHANNEL}'. Total active subscriptions: ${count}`);
        }
    });

    subClient.on('message', (channel, message) => {
        if (channel !== NOTIFICATION_CHANNEL) {
            return;
        }

        try {
            const payload = typeof message === 'string' ? JSON.parse(message) : message;
            const { recipientType, recipientId, targetPermissions, type, message: notifMessage, createdAt } = payload || {};

            const notificationData = {
                type,
                message: notifMessage,
                createdAt: createdAt || new Date().toISOString()
            };

            let handled = false;

            // 1. Direct to user
            if (recipientId) {
                io.to(userRoom(recipientId)).emit(NOTIFICATION_EVENT, notificationData);
                handled = true;
            }

            // 2. Broadcast to users by target permissions
            // No need to also emit to admin_room — admin users already join perm_admin:access
            // and perm_super_admin:access rooms on connect (socketConnectionHandler.js)
            if (Array.isArray(targetPermissions) && targetPermissions.length > 0) {
                targetPermissions.forEach(perm => {
                    const permStr = typeof perm === 'object' && perm ? (perm.value || perm.name || String(perm)) : String(perm);
                    io.to(permRoom(permStr)).emit(NOTIFICATION_EVENT, notificationData);
                });
                handled = true;
            }

            // 3. Backward compatibility for legacy recipientType if not yet handled
            if (!handled && recipientType) {
                const normalizedRecipientType = String(recipientType).toUpperCase();
                if (normalizedRecipientType === 'USER' && recipientId) {
                    io.to(userRoom(recipientId)).emit(NOTIFICATION_EVENT, notificationData);
                    handled = true;
                } else if (normalizedRecipientType === 'ADMIN') {
                    io.to(ADMIN_ROOM).emit(NOTIFICATION_EVENT, notificationData);
                    handled = true;
                }
            }

            if (!handled) {
                logger.warn(`[Notification] Unhandled notification payload: ${JSON.stringify(payload)}`);
            }
        } catch (err) {
            logger.error('❌ Failed to process notification message from Redis:', err.message);
        }
    });

    return subClient;
};

const closeNotificationSubscriber = async () => {
    if (subClient) {
        try {
            await subClient.quit();
            logger.info('Redis notification subscriber connection closed.');
        } catch (err) {
            logger.error('Error closing Redis notification subscriber:', err);
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
    initNotificationSubscriber,
    closeNotificationSubscriber,
    NOTIFICATION_CHANNEL
};
