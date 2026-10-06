# E-Commerce Platform — AI Agent Instructions

## What This Project Is

A microservices-based e-commerce platform. Monorepo with four services: Gateway, Main Server, Chat Server, and Client.

## Read This Before Doing Anything

| Topic                        | File                                  |
|------------------------------|---------------------------------------|
| System design & service map  | [docs/architecture.md](./docs/architecture.md) |
| Coding conventions & testing | [docs/development.md](./docs/development.md)   |
| Code patterns & examples     | [docs/code-patterns.md](./docs/code-patterns.md) |
| Security rules & auth flow   | [docs/security.md](./docs/security.md)         |
| Docker, env vars, CI/CD      | [docs/deployment.md](./docs/deployment.md)     |
| API contract & response shape| [docs/api.md](./docs/api.md)                   |

## Architecture Decisions (Why things are the way they are)

| Decision                             | File                                                         |
|--------------------------------------|--------------------------------------------------------------|
| Why 4 microservices                  | [docs/decisions/001-microservices.md](./docs/decisions/001-microservices.md) |
| Why JWT is validated at Gateway only | [docs/decisions/002-jwt-gateway-auth.md](./docs/decisions/002-jwt-gateway-auth.md) |
| Why Redis cache + dual eviction      | [docs/decisions/003-redis-caching.md](./docs/decisions/003-redis-caching.md) |
| Why no message queue for chat        | [docs/decisions/004-websocket-no-mq.md](./docs/decisions/004-websocket-no-mq.md) |
| Why React Query pages[0] = newest    | [docs/decisions/005-react-query-cache.md](./docs/decisions/005-react-query-cache.md) |

## Services at a Glance

| Service       | Language / Framework    | Port | Directory    |
|---------------|-------------------------|------|--------------|
| Gateway       | Java / Spring Cloud     | 8080 | `gateway/`   |
| Main Server   | Java / Spring Boot 3.5  | 8081 | `server/`    |
| Chat Server   | Node.js / Socket.IO     | 3001 | `server_chat/`|
| Client        | React 18 / Vite / Nginx | 80   | `client/`    |

## Non-Negotiable Rules (Quick Reference)

1. **All traffic goes through the Gateway.** Individual services do not handle CORS. JWT is validated at the Gateway for routing/rate-limiting *and* again in the Main Server's own `AuthenticationFilter` to populate the Spring Security context (`@AuthenticationPrincipal`).
2. **Every HTTP response is `ApiResponse<T>`** — never a raw entity or plain string.
3. **All timestamps are `java.time.Instant`** (UTC ISO-8601). Never `LocalDateTime`.
4. **All cache names come from `CacheConstants.java`** — no magic strings.
5. **No `console.log` in Node.js** — use `logger.js`.
6. **No env var defaults in config files** — write `${VAR}`, not `${VAR:default}`.
7. **DTOs only** — never expose JPA entities directly in API responses.
8. **Migrations via Flyway (server) / Sequelize CLI (chat-server)** — never modify model files without a migration.
9. **`@Transactional` on every state-changing service method** (Spring).
10. **WebSocket auth** — every socket connection must pass through `socketAuthMiddleware` before joining any room.
