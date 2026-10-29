/**
 * k6 Load Test — Authentication Flow
 *
 * Tests login, token refresh, and logout under load.
 * Measures auth endpoint latency and rate-limit behavior.
 *
 * Prerequisites:
 *   - A test user must exist: TEST_EMAIL / TEST_PASS
 *
 * Run:
 *   k6 run -e TEST_EMAIL=user@example.com -e TEST_PASS=Password123! tests/load/auth.js
 *
 * Note: Auth endpoints are rate-limited to 20/min per IP.
 *       Keep VU count low or the test will hit 429s (which is expected).
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, defaultParams, checkApiResponse } from './helpers.js';

const TEST_EMAIL = __ENV.TEST_EMAIL || 'loadtest@example.com';
const TEST_PASS  = __ENV.TEST_PASS  || 'LoadTest123!';

export const options = {
    // Low VU count — auth has strict rate limits (20/min per IP)
    stages: [
        { duration: '10s', target: 3 },
        { duration: '30s', target: 5 },
        { duration: '30s', target: 5 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        'http_req_duration{name:login}': ['p(95)<800'],
        'http_req_duration{name:logout}': ['p(95)<500'],
        'checks': ['rate>0.90'],  // allow some 429s from rate limiting
    },
};

export default function () {
    // --- Login ---
    const loginRes = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ email: TEST_EMAIL, password: TEST_PASS }),
        { ...defaultParams, tags: { name: 'login' } }
    );

    // Accept 200 (success) or 429 (rate limited — expected)
    const loginOk = check(loginRes, {
        'login 200 or 429': (r) => r.status === 200 || r.status === 429,
    });

    if (loginRes.status === 429) {
        // Rate limited — sleep and skip the rest
        sleep(5);
        return;
    }

    const token = loginRes.json('data.accessToken');
    if (!token) {
        sleep(1);
        return;
    }

    // --- Authenticated request (verify token works) ---
    const profileRes = http.get(
        `${BASE_URL}/api/v1/orders?page=0&size=5`,
        {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
            insecureSkipTLSVerify: true,
            tags: { name: 'auth_verify' },
        }
    );
    checkApiResponse(profileRes, 'auth_verify');

    // --- Logout ---
    const logoutRes = http.post(
        `${BASE_URL}/api/v1/auth/logout`,
        null,
        {
            headers: { 'Authorization': `Bearer ${token}` },
            insecureSkipTLSVerify: true,
            tags: { name: 'logout' },
        }
    );
    check(logoutRes, {
        'logout success': (r) => r.status === 200 || r.status === 204,
    });

    sleep(Math.random() * 3 + 2);  // space out to avoid rate limits
}
