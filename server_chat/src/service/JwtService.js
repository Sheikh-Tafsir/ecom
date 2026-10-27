const jwt = require('jsonwebtoken');
const { randomUUID } = require('crypto');

/**
 * Verifies a JWT token and enforces mandatory JTI claim presence.
 * @param {string} token - The JWT token
 * @param {Buffer} secret - The signing secret
 * @returns {object} The decoded JWT payload
 * @throws {Error} If the token is invalid, expired, or lacks a JTI claim
 */
const verifyToken = (token, secret) => {
    const decoded = jwt.verify(
        token,
        secret,
        { algorithms: ['HS256', 'HS384', 'HS512'] }
    );

    if (!decoded.jti) {
        throw new Error("Token lacks mandatory JTI claim");
    }

    return decoded;
}

const parseAccessToken = (token) => {
    return verifyToken(token, Buffer.from(process.env.ACCESS_TOKEN_SECRET, "utf8"));
}

const isAccessTokenValid = (token) => {
    try {
        parseAccessToken(token);
        return true;
    } catch {
        return false;
    }
}

const parseRefreshToken = (token) => {
    return verifyToken(token, Buffer.from(process.env.REFRESH_TOKEN_SECRET, "utf8"));
}

const isRefreshTokenValid = (token) => {
    try {
        parseRefreshToken(token);
        return true;
    } catch {
        return false;
    }
}

const generateAccessToken = (user) => {
    return jwt.sign(
        {
            sub: String(user.id),
            id: user.id,
            name: user.name,
            email: user.email,
            permissions: user.permissions,
            status: user.status || 'ACTIVE'
        },
        Buffer.from(process.env.ACCESS_TOKEN_SECRET, "utf8"),
        { expiresIn: process.env.ACCESS_TOKEN_VALIDITY || '30m', jwtid: randomUUID() }
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
    parseAccessToken,
    isAccessTokenValid,
    parseRefreshToken,
    isRefreshTokenValid,
    generateAccessToken,
    generateRefreshToken,
    getEmailFromRefreshToken,
    getIdFromRefreshToken,
    getExpirationFromRefreshToken
};
