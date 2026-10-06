# ADR 002 — JWT Validation at Gateway Only

**Status:** Accepted  
**Date:** 2024

## Decision

JWT access tokens are validated at **two layers**:

1. **API Gateway** — validates signature + expiry for routing and rate-limiting decisions. Injects user identity headers into downstream requests.
2. **Main Server** — validates the JWT again in its own `AuthenticationFilter` to populate the Spring Security context (`SecurityContextHolder`), enabling `@AuthenticationPrincipal`, `@PreAuthorize`, and `@PostAuthorize` to work.

This is a **defense-in-depth** approach — the server does not blindly trust injected headers from the Gateway.

## Why

- **Single point of enforcement** — security policy is applied in one place. Changing the token algorithm or secret only requires updating the Gateway.
- **Performance** — eliminates redundant cryptographic verification on every downstream service for every request.
- **Simplified downstream services** — `server` and `chat-server` don't need JWT libraries or secret management.

## Implementation

1. `AuthenticationFilter` in the Gateway validates JWT signature and expiry using `ACCESS_TOKEN_SECRET`.
2. On success, the filter injects `X-User-Id`, `X-User-Role` (or equivalent) headers.
3. Downstream services read these headers directly — they never touch the `Authorization: Bearer` header.
4. On failure, the Gateway returns 401 before the request reaches any downstream service.

## Token Revocation

- Tokens can be revoked by writing the token JTI to Redis (via `IdempotencyService`).
- The `AuthenticationFilter` checks Redis on every request for revoked tokens.
- The Gateway has access to the `data` Docker network specifically for this Redis check.

## Trade-offs Accepted

| ✅ Benefit                               | ⚠️ Cost                                                     |
|------------------------------------------|-------------------------------------------------------------|
| Centralized auth policy                  | Gateway becomes a critical availability dependency          |
| Downstream services stay stateless/simple| If Gateway is bypassed internally, requests are unauthenticated |
| Easy secret rotation                     | Header injection must be trusted — internal network must be isolated |

## Constraints

- The `backend` Docker network must be internal-only. No service on `backend` should be reachable from outside the Docker network.
- `chat-server` WebSocket auth (`socketAuthMiddleware`) validates the JWT directly because WebSocket upgrades have a different flow — they do not go through the standard HTTP filter chain.
