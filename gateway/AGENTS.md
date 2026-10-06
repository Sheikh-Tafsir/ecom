# Gateway — Agent Instructions

> For system design, security rules, and deployment see the [shared docs](../docs/).

## What This Service Does

Spring Cloud Gateway — the **sole public entry point** for all API traffic. Handles CORS, JWT validation, rate limiting, and request routing. No business logic lives here.

## Routing Rules

| Route ID          | Path Predicate  | Upstream                  | Notes                                         |
|-------------------|-----------------|---------------------------|-----------------------------------------------|
| `chat-service-ws` | `/socket.io/**` | `ws://chat-server:3001`   | Socket.IO WebSocket upgrade                   |
| `chat-service`    | `/chats/**`     | `http://chat-server:3001` | Chat REST endpoints                           |
| `main-service`    | `/**`           | `http://server:8081`      | Catch-all; has CircuitBreaker + GET retry (×2)|

Route order matters — `chat-service-ws` and `chat-service` are matched before the catch-all `main-service`. Add new routes in `application.yml` above the `main-service` entry.

## What This Service Owns

- Centralized **CORS** configuration (`CORS_ALLOWED_ORIGINS` env var)
- **JWT validation** via `AuthenticationFilter` — validates signature + expiry using `ACCESS_TOKEN_SECRET`, then injects user identity headers downstream
- **Rate limiting** via `IpRateLimiterFilter` — Bucket4j + Redis backing; Caffeine fallback in dev
- **Request logging** via `LoggingFilter` — assigns MDC `requestId`, masks sensitive credentials
- **Token revocation** check against Redis on every authenticated request

## Hard Rules

- Do **not** add business logic here — routing and cross-cutting concerns only.
- Do **not** configure CORS or auth in `server` or `chat-server` — this is the single authority.
- Always return a standardized JSON error if an upstream service is unreachable (not an HTML error page).
- Define response timeouts for every route — no unbounded waits.
- Use Global Filters for any new cross-cutting behavior (logging, header normalization).

## Tech Stack

- Java / Spring Cloud Gateway
- Build: Gradle (`./gradlew bootRun --args='--spring.profiles.active=dev'`)
- Profiles: `dev` (colorized console logs), `prod` (Logstash JSON logs)

## Key Env Vars

| Variable               | Purpose                            |
|------------------------|------------------------------------|
| `ACCESS_TOKEN_SECRET`  | JWT signature validation           |
| `CORS_ALLOWED_ORIGINS` | Allowed client origins             |
| `MAIN_SERVICE_URL`     | `http://server:<port>`             |
| `CHAT_SERVICE_URL`     | `http://chat-server:<port>`        |
| `CHAT_SERVICE_WS_URL`  | `ws://chat-server:<port>`          |
| `REDIS_HOST/PORT/PASSWORD` | Rate limiting + token revocation |
