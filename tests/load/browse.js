/**
 * k6 Load Test — Product Browsing
 *
 * Tests the highest-traffic, unauthenticated endpoints:
 *   - Product listing (paginated)
 *   - Single product detail
 *   - Categories listing
 *   - Banners / CMS content
 *
 * Run:
 *   k6 run tests/load/browse.js
 *   k6 run --vus 50 --duration 2m tests/load/browse.js
 */

import http from 'k6/http';
import { sleep } from 'k6';
import { BASE_URL, defaultParams, defaultThresholds, checkApiResponse } from './helpers.js';

export const options = {
    stages: [
        { duration: '15s', target: 10 },    // warm up
        { duration: '30s', target: 30 },    // ramp to 30 VUs
        { duration: '1m',  target: 30 },    // steady state
        { duration: '15s', target: 50 },    // push higher
        { duration: '30s', target: 50 },    // hold peak
        { duration: '15s', target: 0 },     // ramp down
    ],
    thresholds: {
        ...defaultThresholds,
        'http_req_duration{name:products_list}': ['p(95)<400'],
        'http_req_duration{name:product_detail}': ['p(95)<300'],
    },
};

export default function () {
    // --- Product listing (page 0) ---
    const listRes = http.get(
        `${BASE_URL}/api/v1/products?page=0&size=10`,
        { ...defaultParams, tags: { name: 'products_list' } }
    );
    checkApiResponse(listRes, 'products_list');

    // --- Product detail (use random product from the list if available) ---
    try {
        const products = listRes.json('data.content') || listRes.json('data');
        if (products && products.length > 0) {
            const randomProduct = products[Math.floor(Math.random() * products.length)];
            const productId = randomProduct ? randomProduct.id : null;
            if (productId) {
                const detailRes = http.get(
                    `${BASE_URL}/api/v1/products/${productId}`,
                    { ...defaultParams, tags: { name: 'product_detail' } }
                );
                checkApiResponse(detailRes, 'product_detail');
            }
        }
    } catch (_) {
        // If products list isn't parseable, just continue
    }

    // --- Categories ---
    const catRes = http.get(
        `${BASE_URL}/api/v1/categories`,
        { ...defaultParams, tags: { name: 'categories' } }
    );
    checkApiResponse(catRes, 'categories');

    // --- Banners ---
    const bannerRes = http.get(
        `${BASE_URL}/api/v1/banners`,
        { ...defaultParams, tags: { name: 'banners' } }
    );
    checkApiResponse(bannerRes, 'banners');

    // Simulate user reading time
    sleep(Math.random() * 2 + 1);
}
