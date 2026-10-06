# Main Server — Agent Instructions

> For system design, security rules, and deployment see the [shared docs](../docs/).

## What This Service Does

Spring Boot 3.5 — core e-commerce business logic. Owns products, orders, stock/inventory, users, roles, payments, email, CMS, and reporting.

## Layer Rules

| Layer          | Rule                                                                                 |
|----------------|--------------------------------------------------------------------------------------|
| **Controller** | `@Valid` on all params. Return `ApiResponse<T>` always. Never expose JPA entities.  |
| **Service**    | `@Transactional` on every state-changing method. Business logic lives here only.    |
| **Repository** | Spring Data JPA. Complex queries via JPQL or `@Query`. No raw SQL in services.      |
| **Entity**     | Extend `BaseEntity` (`createdAt`, `updatedAt` as `Instant`). Never return entities in responses. |
| **DTO**        | Separate Request DTO and Response DTO per endpoint. No shared mutable DTOs.          |

## Non-Negotiable Rules

- `java.time.Instant` for all timestamps — **never** `LocalDateTime`.
- All cache names from `CacheConstants.java` — **no magic strings**.
- Dual cache eviction with `@Caching` when a resource appears under multiple cache keys (e.g., public + admin views).
- `@PreAuthorize` with `Permission` enum values for RBAC.
- `@PostAuthorize` for resource ownership checks.
- Map string path/query params to Enums via Spring `Converter` (e.g., `OrderStatusConverter`).
- All mutating financial endpoints require `Idempotency-Key` header support (handled by `IdempotencyAspect`).
- No env var default fallbacks in `application.yaml` — use `${VAR}`, not `${VAR:default}`.

## Caching

- Strategy controlled by `APP_CACHE_STRATEGY` env var: `redis` (prod), `NONE` (CI/test).
- Default TTL: 5 minutes.
- Example:
  ```java
  @Cacheable(value = CacheConstants.CACHE_PRODUCTS, key = "#id")
  @Caching(evict = {
    @CacheEvict(value = CacheConstants.CACHE_PRODUCTS, key = "#id"),
    @CacheEvict(value = CacheConstants.CACHE_PRODUCTS_EDIT, key = "#id")
  })
  ```

## Database Migrations

- Tool: **Flyway** — auto-runs on startup.
- Location: `src/main/resources/db/migration/`
- Naming: `V<N>__<description>.sql` (e.g., `V3__add_discount_coupons.sql`)
- Scripts must be idempotent. Never use destructive DDL on production tables.

## Testing

- Mandatory unit tests for all `@Service` logic.
- Tools: JUnit 5 + Mockito.
- Naming: `givenX_whenY_thenZ`.
- Run: `./gradlew test`

## Tech Stack

- Java 21 / Spring Boot 3.5, Spring Data JPA, Spring Security
- Build: Gradle (`./gradlew bootRun --args='--spring.profiles.active=dev'`)
- Cache: Redis (prod) / Caffeine (dev)
- Profiles: `dev` (colorized logs, Caffeine), `prod` (Logstash JSON, Redis)

## Key Env Vars

| Variable               | Purpose                                                           |
|------------------------|-------------------------------------------------------------------|
| `DB_URL/USERNAME/PASSWORD` | PostgreSQL connection                                         |
| `REDIS_HOST/PORT/PASSWORD` | Cache + rate limiting                                         |
| `MQ_HOST/PORT/USERNAME/PASSWORD` | RabbitMQ (email queues)                               |
| `ACCESS_TOKEN_SECRET`  | Used by the server's own `JwtService` to validate JWTs and populate `@AuthenticationPrincipal` (defense-in-depth — not just trusting Gateway headers) |
| `CLOUDINARY_*`         | Image upload CDN                                                  |
| `SUPABASE_*`           | Alternative image storage                                         |
| `MAIL_SMTP_*`          | Email delivery                                                    |
| `BKASH_*`              | Payment gateway                                                   |
| `APP_CACHE_STRATEGY`   | `redis` or `NONE`                                                 |
