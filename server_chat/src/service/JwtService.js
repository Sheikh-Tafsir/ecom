const jwt = require('jsonwebtoken');

const isAccessTokenValid = (token) => {
    return jwt.verify(
        token,
        Buffer.from(process.env.ACCESS_TOKEN_SECRET, "utf8")
    );
}

module.exports = {
    isAccessTokenValid,
};