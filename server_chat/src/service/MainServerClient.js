const axios = require('axios');
const { logger } = require('../config/logger');

/**
 * MainServerClient — Internal HTTP Client for Main Server RPC
 *
 * Centralizes machine-to-machine internal authentication (X-Internal-Service-Key),
 * base URL configuration, timeouts, and error handling for inter-service communication.
 */
class MainServerClient {
    constructor() {
        this.baseUrl = process.env.MAIN_SERVICE_URL;
        this.internalServiceKey = process.env.INTERNAL_SERVICE_KEY;
        this.defaultTimeoutMs = 5000;
    }

    /**
     * Build standard headers for internal inter-service requests.
     * @private
     * @param {string} [token] - Optional user Bearer token for contextual requests
     * @returns {Record<string, string>}
     */
    _buildHeaders(token) {
        const headers = {
            'Content-Type': 'application/json',
        };

        if (this.internalServiceKey) {
            headers['X-Internal-Service-Key'] = this.internalServiceKey;
        }

        if (token) {
            headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
        }

        return headers;
    }

    /**
     * Fetch a batch of user profiles from Main Server by their IDs.
     * @param {string[]} userIds - Array of user UUIDs
     * @param {string} [token] - Optional JWT token
     * @returns {Promise<Array<{ id: string, name: string, image: string|null, email: string|null }>>}
     */
    async fetchUsersBatch(userIds, token) {
        if (!Array.isArray(userIds) || userIds.length === 0) {
            return [];
        }

        try {
            const url = `${this.baseUrl}/internal/users/batch`;
            const response = await axios.post(
                url,
                userIds,
                {
                    headers: this._buildHeaders(token),
                    timeout: this.defaultTimeoutMs,
                }
            );

            const data = response.data?.data || response.data;
            return Array.isArray(data) ? data : [];
        } catch (err) {
            logger.warn(`MainServerClient: Failed to fetch users batch from ${this.baseUrl}/internal/users/batch: ${err.message}`);
            return [];
        }
    }
}

module.exports = new MainServerClient();
