/**
 * k6 Load Test Suite — E-Commerce Platform
 *
 * Shared configuration, helpers, and auth utilities used by all test scenarios.
 *
 * Usage:
 *   k6 run tests/load/browse.js          # product browsing
 *   k6 run tests/load/auth.js            # login/register flow
 *   k6 run tests/load/order.js           # order placement (authenticated)
 *   k6 run tests/load/chat.js            # WebSocket chat
 *   k6 run tests/load/full-suite.js      # all scenarios combined
 *
 * Environment variables:
 *   BASE_URL    — app URL (default: https://localhost)
 *   TEST_EMAIL  — pre-existing user email for auth tests
 *   TEST_PASS   — password for the test user
 *   ADMIN_EMAIL — pre-existing admin email
 *   ADMIN_PASS  — password for the admin user
 */

import http from 'k6/http';
import { check } from 'k6';

// --- Base URL ---
export const BASE_URL = __ENV.BASE_URL || 'https://localhost';

// --- Common request params ---
export const defaultParams = {
    insecureSkipTLSVerify: true,  // self-signed certs in dev
    headers: { 'Content-Type': 'application/json' },
};

// --- Thresholds (shared defaults) ---
export const defaultThresholds = {
    http_req_duration: ['p(95)<500', 'p(99)<1500'],
    http_req_failed: ['rate<0.01'],
};

// --- Auth helper: login and return access token ---
export function login(email, password) {
    const res = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ email, password }),
        { ...defaultParams, tags: { name: 'login' } }
    );

    const ok = check(res, {
        'login 200': (r) => r.status === 200,
        'login has token': (r) => r.json('data.accessToken') !== undefined,
    });

    if (!ok) {
        console.error(`Login failed: ${res.status} — ${res.body}`);
        return null;
    }

    return res.json('data.accessToken');
}

// --- Multi-user login helper: supports TEST_USERS="u1:p1,u2:p2" or falls back to single user ---
export function loginUsers(defaultEmail, defaultPass) {
    const userListEnv = __ENV.TEST_USERS;
    if (userListEnv) {
        const tokens = [];
        const entries = userListEnv.split(',');
        for (const entry of entries) {
            const parts = entry.trim().split(':');
            const email = parts[0];
            const pass = parts[1] || defaultPass;
            if (email) {
                const t = login(email, pass);
                if (t) tokens.push(t);
            }
        }
        if (tokens.length > 0) return tokens;
    }

    const singleToken = login(defaultEmail, defaultPass);
    return singleToken ? [singleToken] : [];
}

// --- Select a token for the current VU from pool ---
export function getVuToken(tokens, vuIndex = __VU) {
    if (!tokens || tokens.length === 0) return null;
    const idx = Math.max(0, (vuIndex - 1)) % tokens.length;
    return tokens[idx];
}

// --- Auth headers helper ---
export function authHeaders(token) {
    return {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
        },
        insecureSkipTLSVerify: true,
    };
}

// --- Check ApiResponse helper ---
export function checkApiResponse(res, name, expectedStatus = 200) {
    return check(res, {
        [`${name} status ${expectedStatus}`]: (r) => r.status === expectedStatus,
        [`${name} is ApiResponse`]: (r) => {
            try { return r.json('success') !== undefined; }
            catch { return false; }
        },
    });
}
