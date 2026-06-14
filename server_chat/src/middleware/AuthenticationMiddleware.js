const jwt = require('jsonwebtoken');
require('dotenv').config()

const {UNAUTHORIZED, ACCESS_TOKEN_REQUIRED, ACCESS_TOKEN_INVALID} = require('../utils/Messages');
const RuntimeError = require("../common/RuntimeError");

const AuthenticationMiddleware = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader?.split(' ')[1];

    if (!token) {
        return next(new RuntimeError(401, ACCESS_TOKEN_REQUIRED));
    }

    try {
        req.user = jwt.verify(token, process.env.ACCESS_TOKEN_SECRET);
        next();
    } catch (err) {
        return next(new RuntimeError(401, ACCESS_TOKEN_INVALID));
    }
};

module.exports = AuthenticationMiddleware;
