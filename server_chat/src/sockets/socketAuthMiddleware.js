const {ACCESS_TOKEN_REQUIRED, ACCESS_TOKEN_INVALID} = require("../utils/Messages");
const {parseAccessToken} = require("../service/JwtService");
const RedisConfig = require("../config/RedisConfig");
const {logger} = require("../config/logger");

const socketAuthMiddleware = async (socket, next) => {
    try {
        const token = socket.handshake.auth?.token;
        if (!token) {
            logger.error("Access token is required for socket connection.");

            // Use "Unauthorized" so client connect_error handler triggers token refresh retry
            const error = new Error("Unauthorized");
            error.data = {
                status: 401,
                error: ACCESS_TOKEN_REQUIRED
            };

            return next(error);
        }

        const user = parseAccessToken(token);
        const jti = user.jti;

        // Check if token JTI is blacklisted/revoked in Redis
        const cacheKey = `${process.env.REVOKED_TOKENS_PREFIX}${jti}`;

        try {
            const isBlacklisted = await RedisConfig.get(cacheKey);
            if (isBlacklisted) {
                logger.warn(`Access token JTI: ${jti} is revoked/blacklisted for socket connection`);
                // Use "Unauthorized" so client connect_error handler triggers token refresh retry
                const error = new Error("Unauthorized");
                error.data = {
                    status: 401,
                    error: ACCESS_TOKEN_INVALID
                };
                return next(error);
            }
        } catch (redisErr) {
            // Fail closed: deny socket connection when we cannot verify token revocation status.
            // Allowing a potentially revoked token through is a security risk.
            logger.error(`Redis error checking revoked tokens blacklist for JTI ${jti} (socket):`, redisErr.message);

            const error = new Error('Service temporarily unavailable — unable to verify token status');
            error.data = {
                status: 503,
                error: 'Service temporarily unavailable'
            };

            return next(error);
        }

        socket.user = user;
        socket.data.user = socket.user;

        next();
    } catch (err) {
        logger.error("Invalid access token:", err.message);

        // Use "Unauthorized" so client connect_error handler triggers token refresh retry
        const error = new Error("Unauthorized");
        error.data = {
            status: 401,
            error: ACCESS_TOKEN_INVALID
        };

        return next(error);
    }
};

module.exports = socketAuthMiddleware;
