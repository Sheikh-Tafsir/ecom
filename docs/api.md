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

### Chat
| Method | Path                          | Description                          |
|--------|-------------------------------|--------------------------------------|
| GET    | `/api/v1/chat/chats`          | List chats (with unread counts)      |
| GET    | `/api/v1/chat/chats/:id`      | Messages with cursor pagination      |
| POST   | `/api/v1/chat/chats`          | Create new chat                      |

### Payments
| Method | Path                      | Description                              |
|--------|---------------------------|------------------------------------------|
| POST   | `/payment`                | Initiate bKash payment                   |
| GET    | `/payment/callback`       | bKash callback — server-side redirect only |
| GET    | `/payment/query/{id}`     | Query payment status (admin only)        |
| POST   | `/payment/refund`         | Refund payment (admin only)              |

### Notifications (SSE)
| Method | Path                          | Description                                          |
|--------|-------------------------------|------------------------------------------------------|
| GET    | `/notifications/sse-token`    | Generate a one-time SSE ticket (requires Bearer token)|
| GET    | `/notifications/subscribe`    | Open SSE stream via `?ticket=<ticket>` query param   |

> **Why tickets?** Browsers cannot set `Authorization` headers on `EventSource` connections. The client first fetches a short-lived ticket via authenticated REST, then uses it to open the SSE stream.

---

## SSE Events (Server → Client)

| Event            | Trigger                                | Payload               |
|------------------|----------------------------------------|-----------------------|
| `order-update`   | Order status change (admin action)     | `{ orderId, status }` |



## Socket.IO Events

| Event (Client → Server) | Payload                        | Description                    |
|-------------------------|--------------------------------|--------------------------------|
| `send-message`          | `{ chatId, content }`          | Send a chat message            |
| `join-chat`             | `{ chatId }`                   | Join a chat room               |
| `typing`                | `{ chatId }`                   | Emit typing indicator          |

| Event (Server → Client) | Payload                        | Description                    |
|-------------------------|--------------------------------|--------------------------------|
| `receive-message`       | `{ message }`                  | Incoming message broadcast     |
| `user-typing`           | `{ chatId, userId }`           | Typing indicator               |

- Event names use `lowerCamelCase`.
- Rate limited to **5 messages/second** per socket connection.

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
