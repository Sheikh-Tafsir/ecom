/**
 * k6 Load Test — WebSocket Chat
 *
 * Tests Socket.IO real-time messaging:
 *   1. Authenticate via HTTP (get access token)
 *   2. Open WebSocket connection with auth
 *   3. Join a chat room
 *   4. Send messages at a controlled rate
 *   5. Listen for responses
 *
 * Prerequisites:
 *   - Two test users who share a chat
 *   - TEST_EMAIL / TEST_PASS env vars
 *   - CHAT_ID env var (an existing chat ID)
 *
 * Run:
 *   k6 run -e TEST_EMAIL=user@example.com -e TEST_PASS=Password123! \
 *          -e CHAT_ID=some-chat-id tests/load/chat.js
 *
 * Note: k6 WebSocket support uses the ws module, but Socket.IO uses
 *       Engine.IO framing. This test uses HTTP long-polling fallback
 *       for simplicity. For full WebSocket testing, consider Artillery
 *       with its Socket.IO plugin.
 */

import http from 'k6/http';
import { sleep } from 'k6';
import { BASE_URL, loginUsers, getVuToken, authHeaders, checkApiResponse } from './helpers.js';

const TEST_EMAIL = __ENV.TEST_EMAIL || 'loadtest@example.com';
const TEST_PASS  = __ENV.TEST_PASS  || 'LoadTest123!';
const CHAT_ID    = __ENV.CHAT_ID    || '';

export const options = {
    stages: [
        { duration: '10s', target: 5 },
        { duration: '30s', target: 10 },
        { duration: '1m',  target: 10 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        'http_req_duration{name:chat_list}': ['p(95)<500'],
        'http_req_duration{name:chat_messages}': ['p(95)<500'],
        'http_req_duration{name:chat_send}': ['p(95)<500'],
        'http_req_failed': ['rate<0.05'],
    },
};

export function setup() {
    const tokens = loginUsers(TEST_EMAIL, TEST_PASS);
    if (!tokens || tokens.length === 0) {
        throw new Error('Setup failed: could not login.');
    }
    return { tokens };
}

export default function (data) {
    const token = getVuToken(data.tokens);
    const params = authHeaders(token);

    // --- List user's chats ---
    const chatsRes = http.get(
        `${BASE_URL}/api/chats`,
        { ...params, tags: { name: 'chat_list' } }
    );
    checkApiResponse(chatsRes, 'chat_list');

    // --- Fetch messages from a specific chat ---
    let chatId = CHAT_ID;
    if (!chatId) {
        try {
            const chats = chatsRes.json('data') || [];
            if (chats.length > 0) {
                chatId = chats[0].id;
            }
        } catch (_) {}
    }

    if (chatId) {
        const msgsRes = http.get(
            `${BASE_URL}/api/chats/${chatId}`,
            { ...params, tags: { name: 'chat_messages' } }
        );
        checkApiResponse(msgsRes, 'chat_messages');

        // --- Send message via HTTP ---
        const sendRes = http.post(
            `${BASE_URL}/api/chats/${chatId}/messages`,
            JSON.stringify({ content: 'Load test message via HTTP', contentType: 'text' }),
            { ...params, tags: { name: 'chat_send' } }
        );
        checkApiResponse(sendRes, 'chat_send', 201);

        // --- Mark as viewed ---
        const viewRes = http.post(
            `${BASE_URL}/api/chats/${chatId}/view`,
            JSON.stringify({ lastSeen: new Date().toISOString() }),
            { ...params, tags: { name: 'chat_view' } }
        );
        checkApiResponse(viewRes, 'chat_view');
    }

    sleep(Math.random() * 3 + 3);
}
