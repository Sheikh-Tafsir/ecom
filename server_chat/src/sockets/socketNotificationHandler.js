const PubSubService = require('../service/PubSubService');
const { logger } = require('../config/logger');
const RealtimeService = require('../service/RealtimeService');
const { NOTIFICATION_EVENT } = require('./socketEvents');
const { NOTIFICATION_CHANNEL } = require('../constants/redis.constants');

const handleNotificationMessage = (message) => {
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
            RealtimeService.emitToUser(recipientId, NOTIFICATION_EVENT, notificationData);
            handled = true;
        }

        // 2. Broadcast to users by target permissions
        // No need to also emit to admin_room — admin users already join perm_admin:access
        // and perm_super_admin:access rooms on connect (socketConnectionHandler.js)
        if (Array.isArray(targetPermissions) && targetPermissions.length > 0) {
            targetPermissions.forEach(perm => {
                const permStr = typeof perm === 'object' && perm ? (perm.value || perm.name || String(perm)) : String(perm);
                RealtimeService.emitToPermission(permStr, NOTIFICATION_EVENT, notificationData);
            });
            handled = true;
        }

        // 3. Backward compatibility for legacy recipientType if not yet handled
        if (!handled && recipientType) {
            const normalizedRecipientType = String(recipientType).toUpperCase();
            if (normalizedRecipientType === 'USER' && recipientId) {
                RealtimeService.emitToUser(recipientId, NOTIFICATION_EVENT, notificationData);
                handled = true;
            } else if (normalizedRecipientType === 'ADMIN') {
                RealtimeService.emitToAdmins(NOTIFICATION_EVENT, notificationData);
                handled = true;
            }
        }

        if (!handled) {
            logger.warn(`[Notification] Unhandled notification payload: ${JSON.stringify(payload)}`);
        }
    } catch (err) {
        logger.error('❌ Failed to process notification message from Redis:', err.message);
    }
};

const initNotificationSubscriber = (io) => {
    if (io) {
        RealtimeService.init(io);
    }

    return PubSubService.subscribe(NOTIFICATION_CHANNEL, handleNotificationMessage);
};

const closeNotificationSubscriber = async () => {
    PubSubService.unsubscribe(NOTIFICATION_CHANNEL, handleNotificationMessage);
    await PubSubService.close();
};

module.exports = {
    initNotificationSubscriber,
    closeNotificationSubscriber,
    NOTIFICATION_CHANNEL
};
