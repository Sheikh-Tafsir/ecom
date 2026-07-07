const jwt = require('jsonwebtoken');

const ACCESS_TOKEN_SECRET = Buffer.from(process.env.ACCESS_TOKEN_SECRET, "utf8");
const REFRESH_TOKEN_SECRET = Buffer.from(process.env.REFRESH_TOKEN_SECRET, "utf8");

const isAccessTokenValid = (token) => {
    return jwt.verify(
        token,
        ACCESS_TOKEN_SECRET
    );
}

const isRefreshTokenValid = (token) => {
    return jwt.verify(
        token,
        REFRESH_TOKEN_SECRET
    );
}

const generateAccessToken = (user) => {
    return jwt.sign(
        {
            id: user.id,
            name: user.name,
            email: user.email,
            role: user.role,
            permissions: user.permissions
        },
        ACCESS_TOKEN_SECRET,
        { expiresIn: process.env.ACCESS_TOKEN_VALIDITY || '30m' }
    );
}

const generateRefreshToken = (user, exp) => {
    const payload = {
        sub: user.email,
        email: user.email,
        id: user.id
    };

    const options = {};
    if (exp) {
        payload.exp = exp;
    } else {
        options.expiresIn = process.env.REFRESH_TOKEN_VALIDITY || '7d';
    }

    return jwt.sign(
        payload,
        REFRESH_TOKEN_SECRET,
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
