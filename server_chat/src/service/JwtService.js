const jwt = require('jsonwebtoken');
const randomUUID = require('crypto');

const isAccessTokenValid = (token) => {
    return jwt.verify(
        token,
        Buffer.from(process.env.ACCESS_TOKEN_SECRET, "utf8")
    );
}

const isRefreshTokenValid = (token) => {
    return jwt.verify(
        token,
        Buffer.from(process.env.REFRESH_TOKEN_SECRET, "utf8")
    );
}

const generateAccessToken = (user) => {
    return jwt.sign(
        {
            sub: String(user.id),
            id: user.id,
            name: user.name,
            email: user.email,
            permissions: user.permissions
        },
        Buffer.from(process.env.ACCESS_TOKEN_SECRET, "utf8"),
        { expiresIn: process.env.ACCESS_TOKEN_VALIDITY || '30m' }
    );
}

const generateRefreshToken = (user, exp) => {
    const payload = {
        sub: String(user.id),
        jti: randomUUID()
    };

    const options = {};
    if (exp) {
        payload.exp = exp;
    } else {
        options.expiresIn = process.env.REFRESH_TOKEN_VALIDITY || '7d';
    }

    return jwt.sign(
        payload,
        Buffer.from(process.env.REFRESH_TOKEN_SECRET, "utf8"),
        options
    );
}

const getEmailFromRefreshToken = (token) => {
    const decoded = jwt.decode(token);
    return decoded?.sub || decoded?.email;
}

const getIdFromRefreshToken = (token) => {
    const decoded = jwt.decode(token);
    return decoded?.id;
}

const getExpirationFromRefreshToken = (token) => {
    const decoded = jwt.decode(token);
    return decoded?.exp;
}

module.exports = {
    isAccessTokenValid,
    isRefreshTokenValid,
    generateAccessToken,
    generateRefreshToken,
    getEmailFromRefreshToken,
    getIdFromRefreshToken,
    getExpirationFromRefreshToken
};
