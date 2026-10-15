const RedisConfig = require('../config/RedisConfig');
const { logger } = require('../config/logger');
const { NOTIFICATION_EVENT } = require('./socketEvents');

const NOTIFICATION_CHANNEL = 'ecom:notifications';

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
            const { recipientType, recipientId, type, message: notifMessage, createdAt } = payload || {};

            const notificationData = {
                type,
                message: notifMessage,
                createdAt: createdAt || new Date().toISOString()
            };

            const normalizedRecipientType = recipientType ? String(recipientType).toUpperCase() : null;

            if (normalizedRecipientType === 'USER' && recipientId) {
                io.to(`user_${recipientId}`).emit(NOTIFICATION_EVENT, notificationData);
            } else if (normalizedRecipientType === 'ADMIN') {
                io.to('admin_room').emit(NOTIFICATION_EVENT, notificationData);
            } else {
                logger.warn(`[Notification] Unhandled recipientType: ${recipientType}`);
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
