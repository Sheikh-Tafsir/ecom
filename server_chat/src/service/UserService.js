const MainServerClient = require('./MainServerClient');
const CacheService = require('./CacheService');
const { USER_CACHE_TTL, USER_CACHE_PREFIX } = require('../constants/redis.constants');

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

    // 2. Fetch missing users from Main Server via MainServerClient
    if (missingIds.length > 0) {
        const remoteUsers = await MainServerClient.fetchUsersBatch(missingIds, token);
        for (const user of remoteUsers) {
            if (user && user.id) {
                const profile = {
                    id: user.id,
                    name: user.name || "User",
                    image: user.image || null,
                    email: user.email || null,
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

    // 3. Ensure fallback default for any user not resolved (negative caching)
    for (const id of uniqueIds) {
        if (!userMap[id]) {
            userMap[id] = {
                id,
                name: "User",
                image: null,
                email: null,
            };

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
    getUserById,
};
