const {FORBIDDEN_ACCESS} = require('../utils/Messages');
const {errorResponse} = require("../utils/ResponseUtils");

const AuthorizationMiddleware = (...requiredPermissions) => {
    return async (req, res, next) => {
        const user = req.user;

        try {
            const userPermissions = Array.isArray(user.permissions) ? user.permissions : [user.permissions];
            const hasAccess = requiredPermissions.every(permission => userPermissions.includes(permission));

            if (!hasAccess) {
                return errorResponse(res, FORBIDDEN_ACCESS, 403);
            }

            next();
        } catch (error) {
            console.error("Permission check failed", error);
            return errorResponse(res, "Internal error during authorization", 500);
        }
    };
};

module.exports = AuthorizationMiddleware;
