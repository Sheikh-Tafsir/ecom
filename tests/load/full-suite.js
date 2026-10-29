/**
 * k6 Load Test — Full Suite (All Scenarios Combined)
 *
 * Runs all scenarios simultaneously with realistic traffic distribution:
 *   - 60% browsing (unauthenticated)
 *   - 20% authenticated order flow
 *   - 10% chat activity
 *   - 10% auth (login/logout)
 *
 * Prerequisites:
 *   - Test users: TEST_EMAIL / TEST_PASS
 *
 * Run:
 *   k6 run -e TEST_EMAIL=user@example.com -e TEST_PASS=Password123! tests/load/full-suite.js
 *
 *   # With more load:
 *   k6 run -e TEST_EMAIL=user@example.com -e TEST_PASS=Password123! \
 *          --vus 100 --duration 5m tests/load/full-suite.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, defaultParams, loginUsers, getVuToken, authHeaders, checkApiResponse } from './helpers.js';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const TEST_EMAIL = __ENV.TEST_EMAIL || 'loadtest@example.com';
const TEST_PASS  = __ENV.TEST_PASS  || 'LoadTest123!';

export const options = {
    scenarios: {
        // --- Browsing: highest traffic, no auth ---
        browsing: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '15s', target: 15 },
                { duration: '1m',  target: 30 },
                { duration: '30s', target: 50 },
                { duration: '1m',  target: 50 },
                { duration: '15s', target: 0 },
            ],
            exec: 'browseProducts',
        },
        // --- Orders: authenticated, moderate traffic ---
        orders: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '20s', target: 5 },
                { duration: '1m30s', target: 10 },
                { duration: '1m',  target: 10 },
                { duration: '15s', target: 0 },
            ],
            exec: 'placeOrder',
        },
        // --- Chat: authenticated, light traffic ---
        chat: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '20s', target: 3 },
                { duration: '2m',  target: 5 },
                { duration: '25s', target: 0 },
            ],
            exec: 'chatActivity',
        },
        // --- Auth: very low traffic (rate limited) ---
        auth: {
            executor: 'constant-arrival-rate',
            rate: 3,           // 3 iterations per timeUnit
            timeUnit: '10s',   // = ~18/min (below 20/min rate limit)
            duration: '2m30s',
            preAllocatedVUs: 3,
            exec: 'authFlow',
        },
    },
    thresholds: {
        'http_req_duration{name:products_list}': ['p(95)<400'],
        'http_req_duration{name:place_order}': ['p(95)<1000'],
        'http_req_duration{name:login}': ['p(95)<800'],
        'http_req_duration{name:chat_list}': ['p(95)<500'],
        'http_req_failed': ['rate<0.03'],
    },
};

// --- Shared setup: login (supports user pool or single user) ---
export function setup() {
    const tokens = loginUsers(TEST_EMAIL, TEST_PASS);
    if (!tokens || tokens.length === 0) {
        throw new Error('Setup failed: could not login. Set TEST_EMAIL and TEST_PASS.');
    }
    return { tokens };
}

// ======================
// Scenario: Browse
// ======================
export function browseProducts() {
    const listRes = http.get(
        `${BASE_URL}/api/v1/products?page=${Math.floor(Math.random() * 3)}&size=10`,
        { ...defaultParams, tags: { name: 'products_list' } }
    );
    checkApiResponse(listRes, 'products_list');

    // Random product detail
    try {
        const products = listRes.json('data.content') || listRes.json('data');
        if (products && products.length > 0) {
            const p = products[Math.floor(Math.random() * products.length)];
            const productId = p ? p.id : null;
            if (productId) {
                http.get(
                    `${BASE_URL}/api/v1/products/${productId}`,
                    { ...defaultParams, tags: { name: 'product_detail' } }
                );
            }
        }
    } catch (_) {}

    // Categories
    http.get(`${BASE_URL}/api/v1/categories`, { ...defaultParams, tags: { name: 'categories' } });

    sleep(Math.random() * 2 + 1);
}

// ======================
// Scenario: Order
// ======================
export function placeOrder(data) {
    const token = getVuToken(data.tokens);
    const params = authHeaders(token);

    const listRes = http.get(
        `${BASE_URL}/api/v1/products?page=0&size=10`,
        { ...params, tags: { name: 'products_list' } }
    );

    let productId = null;
    try {
        const products = listRes.json('data.content') || listRes.json('data');
        if (products && products.length > 0) {
            productId = products[Math.floor(Math.random() * products.length)].id;
        }
    } catch (_) {}

    if (!productId) { sleep(1); return; }

    const orderRes = http.post(
        `${BASE_URL}/api/v1/orders`,
        JSON.stringify({
            items: [{ productId, quantity: 1 }],
            shippingAddress: { address: 'Load Test', city: 'Dhaka', phone: '01700000000' },
            paymentMethod: 'COD',
        }),
        {
            headers: { ...params.headers, 'Idempotency-Key': uuidv4() },
            insecureSkipTLSVerify: true,
            tags: { name: 'place_order' },
        }
    );
    check(orderRes, { 'order created': (r) => [200, 201].includes(r.status) });

    sleep(Math.random() * 2 + 1);
}

// ======================
// Scenario: Chat
// ======================
export function chatActivity(data) {
    const token = getVuToken(data.tokens);
    const params = authHeaders(token);

    const chatsRes = http.get(
        `${BASE_URL}/api/chats`,
        { ...params, tags: { name: 'chat_list' } }
    );
    checkApiResponse(chatsRes, 'chat_list');

    try {
        const chats = chatsRes.json('data') || [];
        if (chats.length > 0) {
            const chatId = chats[0].id;
            http.get(
                `${BASE_URL}/api/chats/${chatId}`,
                { ...params, tags: { name: 'chat_messages' } }
            );
        }
    } catch (_) {}

    sleep(Math.random() * 3 + 2);
}

// ======================
// Scenario: Auth
// ======================
export function authFlow() {
    const loginRes = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ email: TEST_EMAIL, password: TEST_PASS }),
        { ...defaultParams, tags: { name: 'login' } }
    );

    if (loginRes.status === 429) { sleep(5); return; }

    const token = loginRes.json('data.accessToken');
    if (!token) { sleep(1); return; }

    // Logout
    http.post(`${BASE_URL}/api/v1/auth/logout`, null, {
        headers: { 'Authorization': `Bearer ${token}` },
        insecureSkipTLSVerify: true,
        tags: { name: 'logout' },
    });

    sleep(2);
}
