# Architecture

## System Overview

A monorepo microservices e-commerce platform. All traffic enters through a single gateway; no service is directly exposed except the React client (via Nginx).

## Service Map

```
React Client (Nginx :80)
        │
        ▼
Spring Cloud Gateway (:8080)          ← sole public API entry point
        │                │
        ▼                ▼
Main Server (:8081)   Chat Server (:3001)
(Spring Boot 3.5)     (Node.js / Socket.IO)
        │                │
        ├── PostgreSQL 16 (shared DB, each service owns its own tables)
        ├── Redis 7       (cache · idempotency · rate-limit · pub/sub)
        └── RabbitMQ 3    (async email queues — Main Server only)
```

## Docker Network Isolation

| Network    | Members                              | Purpose                           |
|------------|--------------------------------------|-----------------------------------|
| `frontend` | gateway, client                      | Client ↔ Gateway traffic          |
| `backend`  | gateway, server, chat-server         | Internal service communication    |
| `data`     | server, chat-server, gateway, redis, postgres, rabbitmq | Data layer access |

- **Client** is on `frontend` only — it cannot reach `server` or `chat-server` directly.
- **Gateway** spans `frontend`, `backend`, and `data` — it needs Redis for token revocation checks.

## Request Routing (Gateway Rules)

| Route ID         | Path Predicate   | Upstream                  | Notes                          |
|------------------|------------------|---------------------------|--------------------------------|
| `chat-service-ws`| `/socket.io/**`  | `ws://chat-server:3001`   | WebSocket upgrade              |
| `chat-service`   | `/chats/**`      | `http://chat-server:3001` | Chat REST endpoints            |
| `main-service`   | `/**`            | `http://server:8081`      | All other requests (catch-all) |

The `main-service` catch-all has a CircuitBreaker (`mainServiceCB`) + GET retry (2 retries on `SERVICE_UNAVAILABLE`).

## Data Flow by Feature

### REST API (HTTP)
```
Client → Gateway (JWT validation) → Server/Chat Server → PostgreSQL/Redis → response
```

### Real-time Chat (WebSocket)
```
Client ──WS──▶ Gateway ──/socket.io/**──▶ Chat Server
                                               │
                                          PostgreSQL (persist)
                                               │
                                          Socket.IO broadcast ──▶ recipient Client
```

### SSE Notifications (Server-Sent Events)
```
Client → GET /notifications/sse-token  → Server → short-lived ticket (stored in Redis)
Client → GET /notifications/subscribe?ticket=<ticket> → Server → SseEmitter (long-lived connection)
Server → push events ──▶ Client (order status changes, admin alerts)
```
Note: SSE uses a one-time ticket instead of a Bearer token because browsers cannot set `Authorization` headers on `EventSource` connections.

### Email / Async Jobs
```
Server ──publish──▶ RabbitMQ ──consume──▶ Server (MailConsumer) ──▶ SMTP
```

## Ports (Local Dev)

| Service      | Port |
|--------------|------|
| Gateway      | 8080 |
| Main Server  | 8081 |
| Chat Server  | 3001 |
| Client       | 5173 |
| PostgreSQL   | 5432 |
| Redis        | 6379 |
| RabbitMQ     | 5672 / 15672 (mgmt) |

## Key Technology Decisions

- **Spring Cloud Gateway** — centralized CORS, JWT validation, rate limiting, and request logging. Individual services do not handle CORS.
- **Shared PostgreSQL** — single DB instance; each service owns its schema tables. Chat Server uses Sequelize (Node); Main Server uses Spring Data JPA + Flyway.
- **Redis** — used by all three backend services: Spring Cache (Main Server), Socket.IO adapter pub/sub (Chat Server), rate-limit bucket storage (Gateway + Main Server).
- **No dedicated sync endpoint for chat** — reconnect recovery uses standard `GET /chats/:id?afterId=` forward pagination.
- **Timestamps** — always `java.time.Instant` (UTC ISO-8601) on the JVM side. Never `LocalDateTime`.
