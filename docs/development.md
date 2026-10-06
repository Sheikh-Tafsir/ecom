# Development Guide

> For concrete copy-paste code examples and patterns, see [code-patterns.md](./code-patterns.md).

## Local Setup (Without Docker)

```bash
# Main Server (Spring Boot)
cd server && ./gradlew bootRun --args='--spring.profiles.active=dev'

# Gateway (Spring Cloud Gateway)
cd gateway && ./gradlew bootRun --args='--spring.profiles.active=dev'

# Chat Server (Node.js)
cd server_chat && npm install && npm run dev

# Client (React / Vite)
cd client && npm install && npm run dev
```

- **Dev profile** uses human-readable colorized console logs and a local Caffeine cache fallback (no Redis required).
- All env vars must be provided — no fallback defaults in `application.yaml`. Use a local `.env` or test `@SpringBootTest(properties = {...})`.

## Testing

```bash
# Server unit + integration tests
cd server && ./gradlew test
```

| Layer        | Tooling                     | Convention                              |
|--------------|-----------------------------|-----------------------------------------|
| Server       | JUnit 5 + Mockito           | BDD naming: `givenX_whenY_thenZ`        |
| Chat Server  | Focus on `service/` layer   | e.g., `MessageService.test.js`          |
| Client       | (Vite / ESLint for now)     | Strict ESLint: hook deps, unused imports|

- **Mandatory** unit tests for all `@Service` logic in the Main Server.
- Never skip tests when changing `@Service` or `@Repository` code.

## Coding Conventions

### All Services
- **No magic strings for cache names** — use `CacheConstants.java` constants only.
- **No `console.log`** in Node.js — use `logger.js` utility exclusively.
- **Env vars** — no hardcoded default fallbacks in config files (e.g., write `${REDIS_PORT}`, not `${REDIS_PORT:6379}`).

### Main Server (Spring Boot)
- Every entity must extend `BaseEntity` (`createdAt`, `updatedAt` as `Instant`).
- Always use `java.time.Instant` for timestamps — never `LocalDateTime`.
- Every controller endpoint returns `ApiResponse<T>`.
- Never expose JPA entities directly — use dedicated Request/Response DTOs.
- Apply `@Valid` on all controller method parameters.
- Use `@Transactional` on every state-changing service method.
- Use `@PreAuthorize` with `Permission` enum values for RBAC.
- Use `@PostAuthorize` for resource ownership checks.
- Map string path/query params to Enums via Spring `Converter` (e.g., `OrderStatusConverter`).
- Cache dual eviction: when updating data with both a public and admin view key, use `@Caching` to evict both.

### Chat Server (Node.js)
- All controller methods must be wrapped in `AsyncHandler` middleware.
- Business logic belongs in `service/` — socket event routing belongs in `sockets/`.
- All DB schema changes require a Sequelize CLI migration. Never modify `model/` files without a migration.
- Abstract model access through `common/Repository.js` for base CRUD.
- All HTTP responses must use the `ApiResponse` class.
- Socket events must use `lowerCamelCase` naming (e.g., `receiveMessage`, `userTyping`).

### Client (React)
- Organize features under `src/features/<domain>/` — each folder owns its components, hooks, and services.
- Reusable atomic components go in `src/components/ui/` (Shadcn) or `src/components/common/`.
- Always use `cn(...)` from `lib/utils.js` for conditional Tailwind class merging — never inline styles.
- Centralize React Query keys as `['entity', filters/id]`. Always pluralize base keys (e.g., `['products']`).
- Mutations must have `onSuccess` handlers that invalidate relevant queries.
- Use `PageLoadingOverlay` for full-page transitions; `ButtonLoading` for form submissions.
- Use `BackButton` in all detail/drill-down views.
- Display toast notifications via `toastify(type, message)` from `common/toastify.js` — never raw `alert()`.
- Map backend validation errors to form fields via `handleErrors(error, setError)`.

## Git & CI Workflow

- **Branches:** `main`/`master` = production; `develop` = integration.
- CI runs on push to `main`, `master`, `develop` and on PRs to `main`/`master`.
- **CI jobs (GitHub Actions):**
  1. `server-build` — Gradle build + test for `server` and `gateway` (JDK 21).
  2. `client-build` — `npm ci && npm run build` for client (Node 22).
  3. `chat-server-build` — `npm ci` for chat server (Node 22).
  4. `docker-build` — `docker compose build` (runs after all 3 above pass).
  5. `deploy` — triggers deployment pipeline (runs only on `main`/`master`).
- All CI jobs must pass before merging to `main`.

## Database Migrations (Main Server)

- Location: `server/src/main/resources/db/migration/`
- Tool: Flyway (auto-runs on startup)
- Naming: `V<Version>__<Description>.sql` — e.g., `V5__add_discount_coupons.sql`
- Migration scripts must be idempotent. Never use destructive DDL on production tables.
- Existing migrations: `V1__init_schema.sql`, `V2__index.sql`, `V3__initial_data.sql`, `V4__add_stock_remaining_check_constraint.sql`

## Environment Variables

The root `.env` file is shared across all services via `docker-compose.yml`. Each service consumes only the variables it needs. See [deployment.md](./deployment.md) for the full variable reference.
