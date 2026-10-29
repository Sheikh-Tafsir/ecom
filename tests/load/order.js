/**
 * k6 Load Test — Order Placement
 *
 * Tests the most revenue-critical flow:
 *   1. Login
 *   2. Browse products
 *   3. Place an order (with idempotency key)
 *   4. Check order status
 *
 * Prerequisites:
 *   - Test user with products in stock
 *   - TEST_EMAIL / TEST_PASS env vars
 *
 * Run:
 *   k6 run -e TEST_EMAIL=user@example.com -e TEST_PASS=Password123! tests/load/order.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, defaultParams, loginUsers, getVuToken, authHeaders, checkApiResponse } from './helpers.js';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const TEST_EMAIL = __ENV.TEST_EMAIL || 'loadtest@example.com';
const TEST_PASS  = __ENV.TEST_PASS  || 'LoadTest123!';

export const options = {
    stages: [
        { duration: '10s', target: 3 },
        { duration: '30s', target: 10 },
        { duration: '1m',  target: 10 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        'http_req_duration{name:place_order}': ['p(95)<1000'],
        'http_req_duration{name:products_list}': ['p(95)<400'],
        'http_req_failed': ['rate<0.05'],
    },
};

export function setup() {
    // Login during setup; supports TEST_USERS pool or single user
    const tokens = loginUsers(TEST_EMAIL, TEST_PASS);
    if (!tokens || tokens.length === 0) {
        throw new Error('Setup failed: could not login. Set TEST_EMAIL and TEST_PASS.');
    }
    return { tokens };
}

export default function (data) {
    const token = getVuToken(data.tokens);
    const params = authHeaders(token);

    // --- Browse products ---
    const listRes = http.get(
        `${BASE_URL}/api/v1/products?page=0&size=10`,
        { ...params, tags: { name: 'products_list' } }
    );
    checkApiResponse(listRes, 'products_list');

    let productId = null;
    try {
        const products = listRes.json('data.content') || listRes.json('data');
        if (products && products.length > 0) {
            productId = products[Math.floor(Math.random() * products.length)].id;
        }
    } catch (_) {}

    if (!productId) {
        sleep(1);
        return;
    }

    sleep(Math.random() * 1 + 0.5);  // simulate browsing

    // --- Place order (with idempotency key) ---
    const idempotencyKey = uuidv4();
    const orderPayload = JSON.stringify({
        items: [{ productId: productId, quantity: 1 }],
        shippingAddress: {
            address: 'Load Test Address',
            city: 'Dhaka',
            phone: '01700000000',
        },
        paymentMethod: 'COD',
    });

    const orderRes = http.post(
        `${BASE_URL}/api/v1/orders`,
        orderPayload,
        {
            headers: {
                ...params.headers,
                'Idempotency-Key': idempotencyKey,
            },
            insecureSkipTLSVerify: true,
            tags: { name: 'place_order' },
        }
    );

    const orderOk = check(orderRes, {
        'order 200/201': (r) => [200, 201].includes(r.status),
    });

    // --- Check order list ---
    if (orderOk) {
        const ordersRes = http.get(
            `${BASE_URL}/api/v1/orders?page=0&size=5`,
            { ...params, tags: { name: 'order_list' } }
        );
        checkApiResponse(ordersRes, 'order_list');
    }

    sleep(Math.random() * 2 + 2);
}
