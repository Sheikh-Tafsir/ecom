# Chat Server Module (Real-time Messaging)

## Overview
Node.js service managing real-time WebSocket communication and chat history.

## Tech Stack
- **Runtime:** Node.js, Express.
- **Real-time:** Socket.io.
- **ORM:** Sequelize (PostgreSQL).
- **Validation:** Manual utility-based validation (e.g., `TrimInput`).

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
