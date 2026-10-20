# ADR 006 — Polyglot Fallback Architecture for Redis Outages

**Status:** Accepted  
**Date:** 2026-10-20  

## Context

Redis is used across all microservices (Gateway, Main Server, Chat Server) for multiple distinct concerns: catalog caching, token revocation, idempotency locking, rate limiting, online presence, and real-time messaging pub/sub.

When Redis becomes temporarily unavailable (e.g. network partition, restart, or crash), the platform must respond predictably. A naive architecture might attempt a single "consistent" fallback (e.g. fallback everything to PostgreSQL, or fallback everything to in-memory RAM). 

## Decision

Adopt an **intentionally polyglot fallback architecture** where each feature's failure behavior is strictly governed by its business criticality and risk profile:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. CRITICAL MONEY & INVENTORY  ➜  PostgreSQL is the ONLY source of truth   │
│    (Orders, payments, idempotency must never duplicate)                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ 2. NON-CRITICAL REAL-TIME      ➜  Gracefully Degrade (In-Memory / Hide)     │
│    (Presence dots, typing text: losing them for 5 min hurts nobody)         │
├─────────────────────────────────────────────────────────────────────────────┤
│ 3. SECURITY & CREDENTIALS      ➜  Fail-Closed (Hard Stop)                   │
│    (Revoked tokens: better to show 503 than let an attacker break in)       │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Why Not a Single Consistent Fallback?

1. **Why we CANNOT fall back to PostgreSQL for everything (The Thundering Herd Trap):**
   If all traffic (product caches, typing throttles, presence heartbeats every 60s across thousands of tabs, and rate limit counters) fell back to PostgreSQL during an outage, the database would face thousands of write/lock queries per second. PostgreSQL CPU would saturate, connection pools would exhaust, and the primary database would crash—turning a minor Redis blip into a catastrophic **cascading blackout**.

2. **Why we CANNOT fall back to In-Memory RAM for everything (The Split-Brain Trap):**
   In production, services run multiple instances/containers. Node A and Node B do not share memory. If order idempotency locks fell back to in-memory state, concurrent checkout submissions hitting different nodes would bypass the lock and double-charge customers.

3. **Why Security MUST Fail-Closed:**
   If token revocation checks fell back to "allow request" when Redis is unreachable, an attacker could deliberately DDoS Redis to bypass account bans or reuse revoked JWT tokens. For short blips (< 15 seconds), the Gateway's local Caffeine cache absorbs downtime. Beyond that, it must fail closed with HTTP `503 Service Unavailable`.

## Outage Behavior Matrix

| Concern | Active Storage | Fallback Mechanism | Business Guarantee |
| :--- | :--- | :--- | :--- |
| **Product & Catalog Cache** | Redis `products::*` | `SimpleCacheErrorHandler` catches exception ➜ queries PostgreSQL directly | Browsing & catalog remain online |
| **Order Idempotency** | Redis `idempotency:*` lock | `IdempotencyAspect` logs warning ➜ PostgreSQL unique partial index on `orders.idempotency_key` catches duplicates | Double-orders physically impossible |
| **bKash Payment Tokens** | Redis `bkash:token` | `ResilientCacheService` catches exception ➜ `InMemoryCacheService` (`ConcurrentHashMap`) | Payment token cached locally per instance |
| **Chat User Profiles** | Redis `cache:user:*` | `UserService.js` catches error ➜ internal HTTP `POST /internal/users/batch` to Main Server | User avatars/names hydrate without interruption |
| **Message Rate Limiting** | Redis `ratelimit:msg:*` | `socketMessageHandlers.js` switches to local in-memory sliding window (5 msgs/sec) | Spam prevented without halting messaging |
| **Online Presence** | Redis `presence:*` Sets | `socketPresenceHandler.js` catches error ➜ presence indicators hide (appear offline) | Chat send/receive functions normally |
| **Typing Throttles** | Redis `typing:*` lock | `socketTypingHandler.js` catches error ➜ drops typing broadcast | Message delivery is never blocked |
| **Admin Notifications** | Redis `ecom:notifications` | `NotificationService` catches error ➜ logs warning | Order transaction completes successfully |
| **Token Blacklist Check** | Redis `v1:revokedAccessTokens` | 1. Gateway in-memory Caffeine cache (< 15s)<br>2. **Fail-Closed:** HTTP `503 Service Unavailable` (> 15s) | Revoked or banned sessions never leak |

## Consequences & Trade-Offs

- **Accepted Complexity:** Each service layer implements its own defensive exception handling (`try/catch` or Spring `CacheErrorHandler`).
- **Safety Over Total Availability for Auth:** An extended Redis outage stops new authenticated requests after the 15-second Caffeine TTL, which is preferred over a security breach.
