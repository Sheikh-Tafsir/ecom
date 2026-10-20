# API Contract

## Response Envelope

Every HTTP endpoint — across `server` and `chat-server` — returns a standardized `ApiResponse<T>` JSON object:

```json
{
  "success": true,
  "message": "Human-readable status message",
  "data": { }
}
```

- `data` is `null` on error responses.
- Never return raw entities, arrays, or plain strings as the top-level response body.

---

## Error Response

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "email": "must be a valid email address",
    "password": "must be at least 8 characters"
  }
}
```

- Field-level validation errors go in `errors` (map of field name → message).
- Use HTTP status codes semantically (400 validation, 401 unauthorized, 403 forbidden, 404 not found, 409 conflict, 500 server error).
- The Gateway returns a standardized JSON error if an upstream service is unreachable (not an HTML error page).

---

## Route Prefixes

| Prefix           | Upstream Service  | Notes                              |
|------------------|-------------------|------------------------------------|
| `/chats/**`      | Chat Server       | Routed by gateway `chat-service`   |
| `/socket.io/**`  | Chat Server       | Socket.IO WebSocket handshake      |
| `/**`            | Main Server       | Catch-all for all REST endpoints   |

---

## Pagination

### Cursor-based (Chat Messages)

The `GET /chats/:id` endpoint supports two directions:

| Parameter                     | Direction    | Use Case                         | Query Logic                                      |
|-------------------------------|--------------|----------------------------------|--------------------------------------------------|
| `cursorCreatedAt` + `cursorId`| **Backward** | Infinite scroll up (older msgs)  | `WHERE (createdAt < cursor) ORDER BY createdAt DESC, id DESC` |
| `afterId`                     | **Forward**  | Fetch missed msgs after reconnect| `WHERE id > afterId ORDER BY id ASC`             |

- Default page size: **15 messages**.
- Both directions use `limit + 1` to detect `hasMore`.
- Backward pagination returns DESC then reverses in application code; forward returns ASC directly.
- Cursor key is `message.id` (BIGSERIAL) — monotonically increasing, globally unique, PK-indexed.

### Offset-based (Products, Orders, Users)

Standard `page` + `size` query params used for admin list endpoints.

---

## Authentication Headers

- **Access Token:** `Authorization: Bearer <token>` on all protected endpoints.
- **Refresh Token:** `HttpOnly` cookie named `refreshToken`.
- After Gateway validation, downstream services receive injected user identity headers — they do not re-read the JWT.

---

## Key Domain Endpoints (Reference)

### Auth
| Method | Path                          | Description                   |
|--------|-------------------------------|-------------------------------|
| POST   | `/api/v1/auth/register`       | Register + send OTP           |
| POST   | `/api/v1/auth/verify-otp`     | Verify OTP, activate account  |
| POST   | `/api/v1/auth/login`          | Email/password login          |
| POST   | `/api/v1/auth/refresh`        | Refresh access token          |
| POST   | `/api/v1/auth/logout`         | Revoke tokens                 |
| GET    | `/api/v1/auth/google`         | Google OAuth SSO              |
| POST   | `/api/v1/auth/forgot-password`| Send password reset OTP       |

### Orders
| Method | Path                          | Description                   |
|--------|-------------------------------|-------------------------------|
| POST   | `/api/v1/orders`              | Place order (idempotency key) |
| GET    | `/api/v1/orders`              | List user orders (paginated)  |
| PATCH  | `/api/v1/orders/:id/status`   | Update order status (admin)   |

### Chat & Media
| Method | Path                            | Description                                          |
|--------|---------------------------------|------------------------------------------------------|
| GET    | `/chats`                        | List user conversations (with unread counts)         |
| GET    | `/chats/:id`                    | Messages with cursor pagination (forward/backward)   |
| POST   | `/chats/:id/view`               | Mark messages as seen (`{ lastSeen: ISO }`)          |
| POST   | `/common/upload-signature`      | Generate signed Cloudinary signature for direct upload|

### Payments
| Method | Path                      | Description                              |
|--------|---------------------------|------------------------------------------|
| POST   | `/payment`                | Initiate bKash payment                   |
| GET    | `/payment/callback`       | bKash callback — server-side redirect only |
| GET    | `/payment/query/{id}`     | Query payment status (admin only)        |
| POST   | `/payment/refund`         | Refund payment (admin only)              |

## Real-time Notifications (Socket.IO)

Notifications are pushed over the authenticated Socket.IO connection. The Main Server publishes events to Redis (`ecom:notifications`), and the Chat Server routes them to the appropriate room (`user_${id}` or `admin_room`).

| Event (Server → Client) | Payload                                    | Description                              |
|-------------------------|--------------------------------------------|------------------------------------------|
| `notification`          | `{ type, message, createdAt }`             | Real-time user or admin alert            |

---

## Socket.IO Events

### Client ➜ Server

| Event | Payload | Description |
| :--- | :--- | :--- |
| `send-message` | `{ chatId?, receiverId?, content, contentType, tempId }` | Send message (acknowledges with `{ chatId }`) |
| `join-chat` | `{ chatId }` | Join active room `chat_${chatId}` (ack returns `{ success, presence }`) |
| `leave-chat` | `{ chatId }` | Leave active room `chat_${chatId}` |
| `typing` | `{ chatId }` | Emit typing indicator (throttled to 1 event / 2s) |
| `presence-query` | `{ userIds: [...] }` | Bulk query presence for specific users |
| `group-create-request` | `{ users, chatId }` | Create a new group chat |
| `group-update-request` | `{ users, chatId }` | Add participants to group (admin only) |

### Server ➜ Client

| Event | Payload | Room / Scope | Description |
| :--- | :--- | :--- | :--- |
| `receive-message` | `{ data: { id, content, chatId, senderId, ... } }` | `chat_${chatId}` | Full message broadcast to active chat viewers |
| `chat-updated` | `{ data: { chatId, lastMessage, lastSenderId, ... } }` | `user_${participantId}` | Lightweight sidebar preview & unread badge bump |
| `messages-seen` | `{ data: { chatId, userId, lastSeen } }` | `user_${participantId}` | Real-time seen receipt broadcast |
| `user-presence` | `{ userId, online, lastSeen }` | `user_${contactId}` | Real-time online/offline presence change |
| `user-typing` | `{ chatId, userId, userName }` | `chat_${chatId}` | Typing notification (excludes sender) |
| `group-create-response` | `{ data: chatId }` | `user_${participantId}` | Signal to refresh sidebar chat list |
| `group-update-response` | `{ data: chatId }` | `user_${participantId}` | Signal to refresh sidebar chat list |

- Event names use `kebab-case` strings.
- Rate limited to **20 messages per 10 seconds** per user with in-memory 5 msg/s fallback per socket.

---

## Timestamp Format

All timestamps in API responses are **UTC ISO-8601**:
```
2024-01-15T10:30:00.000Z
```
Never return Unix epoch integers or `LocalDateTime` strings without timezone.

---

## Order Statuses

```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
                                          └→ RETURNED
PENDING → CANCELLED  (by user)
PENDING → REJECTED   (auto-reject after inactivity)
```
