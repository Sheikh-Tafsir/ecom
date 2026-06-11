const {FORBIDDEN_ACCESS} = require('../utils/Messages');
const {UserRole} = require('../utils/Enum');
const {errorResponse} = require("../utils/ResponseUtils");

const AuthorizationMiddleware = (...allowedRoles) => {
    return async (req, res, next) => {
        const user = req.user;

        if (allowedRoles.includes(UserRole.ADMIN)) {
            allowedRoles.push(UserRole.SUPER_ADMIN);
        }

        try {
            // Assuming user.roleValues is an array of roles from JWT
            const userRoles = Array.isArray(user.roleValues) ? user.roleValues : [user.roleValues];
            const hasAccess = userRoles.some(role => allowedRoles.includes(role));

            if (!hasAccess) {
                return errorResponse(res, FORBIDDEN_ACCESS, 403);
            }

            next();
        } catch (error) {
            console.error("Role check failed", error);
            return errorResponse(res, "Internal error during authorization", 500);
        }
    };
};

module.exports = AuthorizationMiddleware;
