const axios = require('axios');
const CacheService = require('./CacheService');
const { logger } = require('../config/logger');

const {USER_CACHE_TTL, USER_CACHE_PREFIX} = require('../constants/redis.constants');
const MAIN_SERVICE_URL = process.env.MAIN_SERVICE_URL;

/**
 * Fetch multiple users by their IDs with Redis caching and HTTP hydration.
 * Uses machine-to-machine X-Internal-Service-Key for internal authentication.
 * @param {string[]} userIds - Array of user UUIDs
 * @param {string} [token] - Optional authorization token
 * @returns {Promise<Record<string, { id: string, name: string, image: string|null, email: string|null }>>}
 */
const getUsersBatch = async (userIds = [], token) => {
    if (!Array.isArray(userIds) || userIds.length === 0) {
        return {};
    }

    const uniqueIds = Array.from(new Set(userIds.filter(Boolean).map(id => String(id))));
    const userMap = {};
    const missingIds = [];

    // 1. Check cache for each user
    for (const id of uniqueIds) {
        const cached = await CacheService.get(`${USER_CACHE_PREFIX}${id}`);
        if (cached) {
            try {
                userMap[id] = JSON.parse(cached);
            } catch {
                missingIds.push(id);
            }
        } else {
            missingIds.push(id);
        }
    }

    // 2. Fetch missing users from Main Server via internal HTTP POST
    if (missingIds.length > 0) {
        try {
            const headers = {
                'Content-Type': 'application/json'
            };
            if (process.env.INTERNAL_SERVICE_KEY) {
                headers['X-Internal-Service-Key'] = process.env.INTERNAL_SERVICE_KEY;
            }
            if (token) {
                headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
            }

            const response = await axios.post(
                `${MAIN_SERVICE_URL}/internal/users/batch`,
                missingIds,
                {
                    headers,
                    timeout: 5000
                }
            );

            const responseData = response.data?.data || response.data;
            if (Array.isArray(responseData)) {
                for (const user of responseData) {
                    if (user && user.id) {
                        const profile = {
                            id: user.id,
                            name: user.name || "User",
                            image: user.image || null,
                            email: user.email || null
                        };
                        userMap[user.id] = profile;

                        // Cache user profile
                        await CacheService.set(
                            `${USER_CACHE_PREFIX}${user.id}`,
                            JSON.stringify(profile),
                            USER_CACHE_TTL
                        );
                    }
                }
            }
        } catch (httpErr) {
            logger.warn(`Failed to fetch users batch from Main Server (${MAIN_SERVICE_URL}/internal/users/batch):`, httpErr.message);
        }
    }

    // 3. Ensure fallback default for any user not resolved
    for (const id of uniqueIds) {
        if (!userMap[id]) {
            userMap[id] = {
                id,
                name: "User",
                image: null,
                email: null
            };

            // Negative caching with short TTL to prevent cache penetration
            await CacheService.set(
                `${USER_CACHE_PREFIX}${id}`,
                JSON.stringify(userMap[id]),
                60
            );
        }
    }

    return userMap;
};

/**
 * Fetch a single user by ID.
 * @param {string} userId - User UUID
 * @param {string} [token] - Optional authorization token
 * @returns {Promise<{ id: string, name: string, image: string|null, email: string|null }>}
 */
const getUserById = async (userId, token) => {
    if (!userId) return null;
    const users = await getUsersBatch([userId], token);
    return users[userId] || { id: userId, name: "User", image: null, email: null };
};

module.exports = {
    getUsersBatch,
    getUserById
};
