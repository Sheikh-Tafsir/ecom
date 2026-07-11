const jwt = require('jsonwebtoken');
require('dotenv').config()

const {UNAUTHORIZED, ACCESS_TOKEN_REQUIRED, ACCESS_TOKEN_INVALID} = require('../utils/Messages');
const RuntimeError = require("../common/RuntimeError");
const REQUEST_ID_HEADER = "X-Request-Id";
const AUTHORIZATION_HEADER = "Authorization";

const AuthenticationMiddleware = (req, res, next) => {
    const requestId = req.header(REQUEST_ID_HEADER);
    console.info(`Request ID: ${requestId}`);

    const authHeader = req.header(AUTHORIZATION_HEADER);
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
        console.debug("No auth token found in request headers");
        return next(new RuntimeError(401, ACCESS_TOKEN_REQUIRED));
    }

    const token = authHeader?.split(' ')[1];

    try {
        req.user = jwt.verify(token, process.env.ACCESS_TOKEN_SECRET);
        next();
    } catch (err) {
        console.error("Invalid or expired JWT token", err);
        return next(new RuntimeError(401, ACCESS_TOKEN_INVALID));
    }
};

module.exports = AuthenticationMiddleware;
