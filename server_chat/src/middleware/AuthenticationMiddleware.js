const {ACCESS_TOKEN_REQUIRED, ACCESS_TOKEN_INVALID} = require('../utils/Messages');
const RuntimeError = require("../common/RuntimeError");
const RedisConfig = require("../config/RedisConfig");
const {logger} = require("../config/logger");
const {parseAccessToken} = require("../service/JwtService");
const REQUEST_ID_HEADER = "X-Request-Id";
const AUTHORIZATION_HEADER = "Authorization";

const AuthenticationMiddleware = async (req, res, next) => {
    const requestId = req.header(REQUEST_ID_HEADER);
    logger.info(`Request ID: ${requestId}`);

    const authHeader = req.header(AUTHORIZATION_HEADER);
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
        logger.debug("No auth token found in request headers");
        return next(new RuntimeError(401, ACCESS_TOKEN_REQUIRED));
    }

    const token = authHeader.split(' ')[1];

    try {
        const decoded = parseAccessToken(token);
        const jti = decoded.jti;

        // Check if token JTI is blacklisted/revoked in Redis
        const cacheKey = `${process.env.REVOKED_TOKENS_PREFIX}${jti}`;

        try {
            const isBlacklisted = await RedisConfig.get(cacheKey);
            if (isBlacklisted) {
                logger.warn(`Access token JTI: ${jti} is revoked/blacklisted`);
                return next(new RuntimeError(401, ACCESS_TOKEN_INVALID));
            }
        } catch (redisErr) {
            // Fail closed: deny access when we cannot verify token revocation status.
            // Allowing a potentially revoked token through is a security risk.
            logger.error(`Redis error checking revoked tokens blacklist for JTI ${jti}:`, redisErr.message);
            return next(new RuntimeError(503, 'Service temporarily unavailable — unable to verify token status'));
        }

        req.user = decoded;
        next();
    } catch (err) {
        logger.error("Invalid or expired JWT token", err);
        return next(new RuntimeError(401, ACCESS_TOKEN_INVALID));
    }
};

module.exports = AuthenticationMiddleware;
