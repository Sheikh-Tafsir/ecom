# Security

## Authentication Architecture

### Token Strategy
- **Access Token** — short-lived JWT; validated at the Gateway. Never validated again in downstream services.
- **Refresh Token** — stored as an `HttpOnly` cookie (`refreshToken`). Used to issue new access tokens.
- Both token secrets are in env vars: `ACCESS_TOKEN_SECRET`, `REFRESH_TOKEN_SECRET`.

### JWT Validation Flow
```
Client ──Bearer token──▶ Gateway (AuthenticationFilter)
                              │
                         validates JWT signature + expiry
                              │
                         injects user identity headers
                              │
                         forwards to server / chat-server
```
- The `AuthenticationFilter` in the Gateway parses the JWT, validates the signature, and populates the Spring `SecurityContext`.
- Downstream services (`server`, `chat-server`) trust the injected headers — they do **not** re-validate the JWT signature.
- Individual microservices must **never** configure their own CORS or auth layers.

### Socket.IO Authentication
- All WebSocket connections pass through `socketAuthMiddleware` before being allowed to join any room.
- On `connect_error` with "Unauthorized", the client attempts an access token refresh via `socket.js`.

### Token Revocation
- On logout or account suspension, tokens are invalidated via Redis (`SET NX PX` atomic lock with key `v1:revokedAccessTokens::{jti}`).
- The Gateway has direct access to the `data` network for Redis-based revocation checks.
- **Fail-Closed Resilience:** Gateway holds an in-memory Caffeine cache (valid JTIs: 15s TTL; revoked JTIs: 10m TTL) to absorb short Redis hiccups. If Redis is down for longer than 15 seconds, the Gateway **fails closed** with HTTP `503 Service Unavailable`. It never fails open, ensuring revoked or banned tokens cannot be used during an outage.
- Chat Server's `socketAuthMiddleware` also validates the `jti` against Redis and fails closed on connection handshake.

---

## Authorization (RBAC)

### Server-Side Enforcement
- Use `@PreAuthorize` with `Permission` enum values for role-based access:
  ```java
  @PreAuthorize("hasAuthority(T(com.example.Permission).ADMIN_ACCESS.getValue())")
  ```
- Use `@PostAuthorize` for resource ownership checks (data-level security):
  ```java
  @PostAuthorize("returnObject.user.id == principal.id")
  ```

### Roles & Permissions
| Role / Permission      | Access Level                                  |
|------------------------|-----------------------------------------------|
| `ADMIN_ACCESS`         | Full system administration                    |
| `DELIVERY_MAN_ACCESS`  | Order status updates (shipped/delivered)      |
| `CMS_ACCESS`           | Banner, blog, FAQ management                  |

- **User Moderation:** Suspend, ban, or delete accounts triggers instant cache eviction for that user.

---

## CORS

- In production, CORS is effectively **eliminated** by same-origin routing through `nginx-proxy` — all traffic (API, WebSocket, and static assets) is served from the same origin.
- The Gateway's CORS configuration (`CORS_ALLOWED_ORIGINS` = `CLIENT_URL`) is retained as a **defense-in-depth safety net** for local development and as a fallback if requests somehow bypass the proxy.
- Individual services (`server`, `chat-server`) do NOT configure CORS independently.

---

## Rate Limiting

- Implemented via `IpRateLimiterFilter` using **Bucket4j** with Redis backing.
- **Dev/test mode** falls back to local Caffeine cache.
- True client IPs are extracted via `RequestUtil.getClientIp()` which reads `X-Forwarded-For` and `X-Real-IP` headers (reverse proxy / Docker gateway aware).
- `nginx-proxy` sets both `X-Real-IP` and `X-Forwarded-For` headers, ensuring the Gateway always receives the true client IP for rate limiting.
- **Chat Server Rate Limiting:**
  - Message send: Redis sliding window of **20 messages per 10 seconds** per user (`ratelimit:msg:{userId}`). Falls back to an in-memory limit of 5 msgs/sec per socket if Redis is unavailable.
  - Typing indicators: Throttled to **1 event per 2 seconds** per user per chat via Redis `NX` (`typing:{chatId}:{userId}`). Verified via in-memory room authorization (`socket.rooms.has`).

---

## Idempotency

- All mutating financial/critical-state endpoints (`POST /orders`, `POST /payment`) support the `Idempotency-Key` request header.
- **Dual-Layer Protection:**
  1. **Fast Concurrency Defense (Redis):** Handled by `IdempotencyAspect` + `IdempotencyService` using Redis atomic locks (`SET NX PX` 30s) and cached response payloads. Payload hash mismatches return HTTP `422 Unprocessable Entity`.
  2. **Hard Persistence Defense (PostgreSQL):** Flyway migration `V5` adds `idempotency_key` with a partial unique index `uq_orders_user_idempotency` on `orders (user_id, idempotency_key) WHERE idempotency_key IS NOT NULL`.
  3. **Outage Resilience:** If Redis is down, `IdempotencyAspect` catches `RedisConnectionFailureException`, logs a warning, and falls back directly to the database constraint. Duplicate submissions are strictly prevented even during complete Redis outages.

---

## Request Logging & Credential Masking

- `LoggingFilter` assigns a distributed MDC `requestId` for tracing across services.
- Sensitive credentials are **automatically masked** in logs by the filter before writing.
- Do not log raw request bodies that may contain passwords, tokens, or payment data.

---

## Infrastructure Security

- **`nginx-proxy`** is the **sole public entry point** — it exposes `0.0.0.0:${APP_PORT:-80}:80` on the host, making it the only container reachable from the internet.
- **`gateway`** is bound to `127.0.0.1:8080` — accessible for local development only, not publicly exposed.
- **`client`** uses `expose: 80` (internal Docker network only) — **no host port** is mapped at all.
- PostgreSQL, Redis, and RabbitMQ are on the internal `data` network — **never exposed** to the `frontend` network.
- No data-layer service has a host-mapped port in production.
- Redis requires password auth (`REDIS_PASSWORD` env var, `--requirepass` flag).
- Database backup files are AES-256-CBC encrypted at rest (`BACKUP_ENCRYPTION_KEY`). **Change this before production deployment.**
- `BACKUP_ENCRYPTION_KEY` default value in `.env` (`change-me-before-production`) must be replaced before any production deployment.

---

## Payment Security (bKash)

- bKash credentials (`BKASH_APP_KEY`, `BKASH_APP_SECRET`, `BKASH_USERNAME`, `BKASH_PASSWORD`) are environment-only — never committed to source.
- Callback URL is server-side (`/api/payments/bkash/callback`) — never client-side.
- Current integration uses the **sandbox** environment. Swap `BKASH_BASE_URL` to the production URL for live payments.

---

## Threat Model Summary

| Threat                          | Mitigation                                              |
|---------------------------------|---------------------------------------------------------|
| Stolen JWT                      | Short expiry + Redis revocation on logout               |
| Unauthorized API access         | Gateway `AuthenticationFilter` + `@PreAuthorize` RBAC  |
| Cross-site data leakage         | Same-origin via Nginx proxy; CORS at Gateway as defense-in-depth |
| Duplicate payments              | Idempotency key + Redis atomic lock                     |
| IP-based abuse / DDoS           | Bucket4j rate limiting per IP with Redis backing        |
| WebSocket abuse                 | `socketAuthMiddleware` + 5 msg/s rate limit             |
| Log credential exposure         | `LoggingFilter` masks sensitive fields                  |
| Backup data breach              | AES-256-CBC encryption at rest                          |
