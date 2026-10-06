# Chat Server — Agent Instructions

> For system design, security rules, and deployment see the [shared docs](../docs/).

## What This Service Does

Node.js / Socket.IO service — owns real-time chat messaging and chat history. Handles WebSocket connections and HTTP endpoints for message pagination and chat management.

## Layer Rules

| Layer          | Rule                                                                                      |
|----------------|-------------------------------------------------------------------------------------------|
| **Controller** | Every method wrapped in `AsyncHandler`. Return `ApiResponse` class always.               |
| **Service**    | Business logic lives here (`service/`). No socket logic in services.                     |
| **Sockets**    | Socket event routing lives in `sockets/`. No business logic in socket handlers.          |
| **Repository** | Extend `common/Repository.js` for base CRUD. Complex queries extend it further.          |
| **Model**      | Never modify model files without a corresponding Sequelize CLI migration.                |

## Non-Negotiable Rules

- **No `console.log`** anywhere — use `logger.js` exclusively.
- All HTTP responses use the `ApiResponse` class — never raw objects or strings.
- All socket connections pass through `socketAuthMiddleware` before joining any room.
- Socket event names use `lowerCamelCase` (e.g., `receiveMessage`, `userTyping`).
- Rate limit: **5 messages/second per socket** — enforced at socket layer.
- No env var default fallbacks in config — use `${VAR}` not `${VAR ?? 'default'}`.

## Database Migrations

- Tool: **Sequelize CLI** — run migrations before deploying model changes.
- Never modify a `model/` file without a corresponding migration file.
- Commands:
  ```bash
  npx sequelize-cli migration:generate --name add-column-to-messages
  npx sequelize-cli db:migrate
  ```

## Messaging Architecture

```
PostgreSQL = source of truth (every message persisted before broadcast)
WebSocket  = real-time delivery to connected clients
HTTP       = initial load + reconnect recovery
```

### Room Structure
- `user_${userId}` — per-user room for cross-node targeting
- `chat_${chatId}` — per-chat room for message broadcast
- Room setup in `sockets/socketConnectionHandler.js`

### Reconnect Recovery (no dedicated sync endpoint)
1. Client fires `invalidateQueries(['chats'])` on reconnect
2. If chat open: `GET /chats/:id?afterId=<lastMsgId>` fetches only missed messages
3. If > 15 missed: full refetch via `invalidateQueries(['selected_chat', id])`

## Message Pagination (`GET /chats/:id`)

| Param                          | Direction    | Query                                                   |
|--------------------------------|--------------|---------------------------------------------------------|
| `cursorCreatedAt` + `cursorId` | Backward     | `WHERE createdAt < cursor ORDER BY createdAt DESC, id DESC` |
| `afterId`                      | Forward      | `WHERE id > afterId ORDER BY id ASC`                    |

- Default page size: **15**. Use `limit + 1` for `hasMore` detection.

## Testing

- Unit tests focus on `service/` logic (e.g., `MessageService.test.js`).
- Validate socket event emission and room isolation during development.
- Run: `npm run dev` (development), `npm start` (production)

## Tech Stack

- Node.js / Express / Socket.IO
- ORM: Sequelize (PostgreSQL)
- Run: `npm install && npm run dev`

## Key Env Vars

| Variable               | Purpose                              |
|------------------------|--------------------------------------|
| `PORT`                 | Server port (default: 3001)          |
| `DB_URL/USERNAME/PASSWORD` | PostgreSQL connection            |
| `REDIS_HOST/PORT/PASSWORD` | Socket.IO Redis adapter (pub/sub)|
| `ACCESS_TOKEN_SECRET`  | WebSocket JWT validation             |
| `CORS_ALLOWED_ORIGINS` | Client origin (set by Gateway)       |
