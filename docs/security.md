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
- On logout or account suspension, tokens are invalidated via Redis (`SET NX PX` atomic lock).
- The Gateway has direct access to the `data` network for Redis-based revocation checks.

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

- Configured **once** in the Gateway only (`CORS_ALLOWED_ORIGINS` = `CLIENT_URL`).
- Individual services (`server`, `chat-server`) do NOT configure CORS independently.
- Allowed origins are controlled by the `CORS_ALLOWED_ORIGINS` environment variable.

---

## Rate Limiting

- Implemented via `IpRateLimiterFilter` using **Bucket4j** with Redis backing.
- **Dev/test mode** falls back to local Caffeine cache.
- True client IPs are extracted via `RequestUtil.getClientIp()` which reads `X-Forwarded-For` and `X-Real-IP` headers (reverse proxy / Docker gateway aware).
- Chat Server: hard limit of **5 WebSocket messages per second** per socket connection.

---

## Idempotency

- All mutating financial/critical-state endpoints support the `Idempotency-Key` request header.
- Handled by `IdempotencyAspect` + `IdempotencyService` using Redis atomic locks (`SET NX PX`).
- Protects against duplicate order submissions and payment double-charges caused by network retries.

---

## Request Logging & Credential Masking

- `LoggingFilter` assigns a distributed MDC `requestId` for tracing across services.
- Sensitive credentials are **automatically masked** in logs by the filter before writing.
- Do not log raw request bodies that may contain passwords, tokens, or payment data.

---

## Infrastructure Security

- PostgreSQL, Redis, and RabbitMQ are on the `data` network — **not exposed** to the `frontend` network.
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
| Cross-site data leakage         | Centralized CORS at Gateway only                        |
| Duplicate payments              | Idempotency key + Redis atomic lock                     |
| IP-based abuse / DDoS           | Bucket4j rate limiting per IP with Redis backing        |
| WebSocket abuse                 | `socketAuthMiddleware` + 5 msg/s rate limit             |
| Log credential exposure         | `LoggingFilter` masks sensitive fields                  |
| Backup data breach              | AES-256-CBC encryption at rest                          |
