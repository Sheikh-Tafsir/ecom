const {FORBIDDEN_ACCESS, SOMETHING_WENT_WRONG} = require('../utils/Messages');
const RuntimeError = require("../common/RuntimeError");
const {logger} = require("../config/logger");

const AuthorizationMiddleware = (...requiredPermissions) => {
    return async (req, res, next) => {
        try {
            const user = req.user;

            if (!user || !user.permissions) {
                return next(new RuntimeError(401, "Authentication Required"));
            }

            const userPermissions = Array.isArray(user.permissions) ? user.permissions : [user.permissions];
            const hasAccess = requiredPermissions.every(permission => userPermissions.includes(permission));

            if (!hasAccess) {
                return next(new RuntimeError(403, FORBIDDEN_ACCESS));
            }

            next();
        } catch (error) {
            logger.error("Permission check failed", error);
            return next(new RuntimeError(500, SOMETHING_WENT_WRONG));
        }
    };
};

module.exports = AuthorizationMiddleware;
