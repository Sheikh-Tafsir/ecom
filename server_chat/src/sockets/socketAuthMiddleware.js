const {ACCESS_TOKEN_REQUIRED, ACCESS_TOKEN_INVALID} = require("../utils/Messages");
const {isAccessTokenValid} = require("../service/JwtService");
const RedisConfig = require("../config/RedisConfig");

const socketAuthMiddleware = async (socket, next) => {
    try {
        const token = socket.handshake.auth?.token;
        if (!token) {
            console.error("Access token is required for socket connection.");

            const error = new Error(ACCESS_TOKEN_REQUIRED);
            error.data = {
                status: 401,
                error: ACCESS_TOKEN_REQUIRED
            };

            return next(error);
        }

        const user = isAccessTokenValid(token);
        const jti = user.jti;

        // Enforce mandatory JTI claim presence for defense-in-depth security
        if (!jti) {
            console.warn("Access token lacks a mandatory JTI claim for socket connection");
            const error = new Error(ACCESS_TOKEN_INVALID);
            error.data = {
                status: 401,
                error: ACCESS_TOKEN_INVALID
            };
            return next(error);
        }

        // Check if token JTI is blacklisted/revoked in Redis
        const revokedTokensPrefix = process.env.REVOKED_TOKENS_PREFIX || 'v1:revokedAccessTokens::';
        const cacheKey = `${revokedTokensPrefix}${jti}`;

        try {
            const isBlacklisted = await RedisConfig.get(cacheKey);
            if (isBlacklisted) {
                console.warn(`Access token JTI: ${jti} is revoked/blacklisted for socket connection`);
                const error = new Error(ACCESS_TOKEN_INVALID);
                error.data = {
                    status: 401,
                    error: ACCESS_TOKEN_INVALID
                };
                return next(error);
            }
        } catch (redisErr) {
            // Fail closed: deny socket connection when we cannot verify token revocation status.
            // Allowing a potentially revoked token through is a security risk.
            console.error(`Redis error checking revoked tokens blacklist for JTI ${jti} (socket):`, redisErr.message);

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
        console.error("Invalid access token:", err.message);

        const error = new Error(ACCESS_TOKEN_INVALID);
        error.data = {
            status: 401,
            error: ACCESS_TOKEN_INVALID
        };

        return next(error);
    }
};

module.exports = socketAuthMiddleware;
