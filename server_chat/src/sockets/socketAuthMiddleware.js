const {ACCESS_TOKEN_REQUIRED, ACCESS_TOKEN_INVALID} = require("../utils/Messages");
const {isAccessTokenValid} = require("../service/JwtService");
const {buildErrorResponse} = require("../utils/ResponseUtils"); // Assuming this is needed for error responses

const socketAuthMiddleware = (socket, next) => {
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

        socket.user = isAccessTokenValid(token);

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
