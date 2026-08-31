# Chat Server Module (Real-time Messaging)

## Overview
Node.js service managing real-time WebSocket communication and chat history.

## Tech Stack
- **Runtime:** Node.js, Express.
- **Real-time:** Socket.io.
- **ORM:** Sequelize (PostgreSQL).
- **Validation:** Manual utility-based validation (e.g., `TrimInput`).

## Chat Messaging Architecture

### Data Flow Principles
```
PostgreSQL = source of truth
WebSocket  = real-time delivery (connected clients only)
HTTP       = initial data + recovery after disconnect
```

### 1. Real-time Messaging (WebSocket)
```
React → WS send-message → Node → PostgreSQL → WS receive-message → recipient React
```
- Messages are sent via Socket.IO (`send-message` event) in `sockets/socketMessageHandlers.js`.
- Saved to PostgreSQL via `MessageService.sendMessage`, then broadcast to the chat room via `receive-message`.
- **No RabbitMQ** — direct Socket.IO delivery only.
- Rate limited to 5 messages/second per socket.

### 2. Message Pagination (HTTP)
The `GET /chats/:id` endpoint supports two pagination directions:

| Parameter | Direction | Use Case | Query |
|-----------|-----------|----------|-------|
| `cursorCreatedAt` + `cursorId` | **Backward** | Infinite scroll up (older messages) | `WHERE (createdAt < cursor) ORDER BY createdAt DESC, id DESC` |
| `afterId` | **Forward** | Fetch missed messages after reconnect | `WHERE id > afterId ORDER BY id ASC` |

- Default page size: 15 messages.
- Both directions use `limit + 1` pattern for `hasMore` detection.
- Backward pagination returns DESC then reverses; forward returns ASC directly.

### 3. Reconnect Synchronization
When a WebSocket disconnects and reconnects, messages sent during the gap are already in PostgreSQL. Recovery flow:

```
WebSocket reconnect
    ↓
invalidateQueries(['chats'])     → refetch chat list (unread counts, last messages)
    ↓
Is a chat currently open?
    ↓ yes
GET /chats/:id?afterId=<lastMsgId>  → fetch only missed messages
    ↓
≤ 15 missed  → append to cache with ID dedup
> 15 missed  → invalidateQueries (full refetch)
```

- **No dedicated `/sync` endpoint** — chat list refetch provides metadata recovery; forward pagination provides message recovery.
- **Cursor strategy:** `message.id` (BIGSERIAL) — monotonically increasing, globally unique, PK-indexed.
- **Race conditions:** ID-based deduplication prevents duplicates when both sync HTTP and WebSocket deliver the same message.

### 4. Chat Rooms & Presence
- Each user joins `user_${userId}` room on connect (cross-node targeting).
- Each chat has a `chat_${chatId}` room for message broadcast.
- Room membership is set up in `sockets/socketConnectionHandler.js`.

## Architectural Mandates

### 1. Socket.io Event Patterns
- **Namespacing:** Use room-based communication for individual and group chats.
- **Event Naming:** Use `lowerCamelCase` for event names (e.g., `receiveMessage`, `userTyping`).
- **Connection Logic:** All socket connections must pass through `socketAuthMiddleware` to verify JWTs before allowing room joins.

### 2. Controller & Service Layer
- **Async Handling:** All controller methods MUST be wrapped in the `AsyncHandler` middleware to ensure errors are caught and passed to `ErrorHandler.js`.
- **Logic Separation:** Business logic (e.g., creating a message receipt) belongs in the `service/` layer, while socket event routing lives in `sockets/`.

### 3. Database & Persistence
- **Migrations:** Use Sequelize CLI for all schema changes. Never modify `model/` files without a corresponding migration.
- **Repository Pattern:** Abstract model access into the `common/Repository.js` for base CRUD, extending for complex queries.

### 4. API Consistency
- **Response Format:** All HTTP responses must use the `ApiResponse` class for a standardized JSON structure.
- **Logging:** Use the `logger.js` utility for all application logs. Avoid `console.log`.

## Testing & Validation
- **Unit Testing:** Focus on `service/` logic (e.g., `MessageService.test.js`).
- **Socket Testing:** Validate event emission and room isolation during development.
