# ADR 003 — Redis Caching Strategy

**Status:** Accepted  
**Date:** 2024

## Decision

Use Redis 7 as the distributed cache for the Main Server with a 5-minute default TTL, Caffeine as the local fallback in dev/test, and a dual-eviction pattern for data that appears in multiple cache keys.

## Why

- **Distributed cache** — multiple server instances (if scaled horizontally) share the same cache state. Caffeine (in-process) would cause stale cache across replicas.
- **Redis is already required** for idempotency and rate limiting — adding caching adds no new infrastructure dependency.
- **TTL of 5 minutes** balances freshness vs. DB load for typical product/category read patterns.

## Implementation Rules

1. **All cache names must come from `CacheConstants.java`** — no magic strings:
   ```java
   @Cacheable(value = CacheConstants.CACHE_PRODUCTS, key = "#id")
   ```

2. **Dual eviction with `@Caching`** — when a resource appears under multiple cache keys (e.g., public product list and admin product edit view), evict both:
   ```java
   @Caching(evict = {
     @CacheEvict(value = CacheConstants.CACHE_PRODUCTS, key = "#id"),
     @CacheEvict(value = CacheConstants.CACHE_PRODUCTS_EDIT, key = "#id")
   })
   ```

3. **Key-based eviction preferred** — use `key = "#id"` over `allEntries = true` unless a bulk operation makes targeted eviction impractical.

4. **Cache strategy is configurable** via `APP_CACHE_STRATEGY` env var:
   - `redis` — production default
   - `NONE` — CI and unit tests (no Redis required)

## Trade-offs Accepted

| ✅ Benefit                        | ⚠️ Cost                                         |
|-----------------------------------|-------------------------------------------------|
| Reduced DB load for read-heavy ops| Cache invalidation complexity (dual eviction)   |
| Shared across server replicas     | Redis becomes a runtime dependency              |
| Configurable strategy for testing | TTL means possible 5-minute stale window        |
