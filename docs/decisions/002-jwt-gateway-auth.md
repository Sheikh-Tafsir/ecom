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

## Amendment — 2026-10-25 (implementation as audited)

The "Implementation" section above describes header injection that was never built (or was later removed). Actual behaviour:

1. The Gateway verifies signature, expiry and JTI revocation, but **does not inject** `X-User-Id` / `X-User-Role` headers and forwards the `Authorization` header unchanged.
2. The Main Server and the Chat Server **each re-verify the JWT** themselves; neither reads identity headers. Bypassing the Gateway therefore does not bypass authentication.
3. Consequently the "downstream services don't need JWT libraries or secrets" benefit does not hold: all three services hold `ACCESS_TOKEN_SECRET`. Because the algorithm is HMAC, each of them can also mint tokens. Moving to an asymmetric algorithm (RS256/EdDSA, private key only on the Main Server) would restore a single signing authority.
4. Per-user revocation (ban, password reset) is checked only by the Main Server; the Gateway and Chat Server check per-token JTIs only.

See [docs/security.md](../security.md#jwt-validation-flow) for the current flow and [the 2026-10-25 audit](../../.context/2026-10-25-system-audit.md) (F-08, F-16).
