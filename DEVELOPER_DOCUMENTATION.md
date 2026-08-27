# E-Commerce Architecture & Developer Guide

## 1. System Architecture Overview

The system is organized into a scalable, decoupled microservices/service-oriented architecture:

```
                  ┌──────────────────────┐
                  │    React SPA Client  │ (Vite / React 18 / Tailwind / Nginx)
                  └──────────┬───────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │  Spring Cloud Gateway│ (Port 8080)
                  └─────┬──────────┬─────┘
                        │          │
         ┌──────────────┘          └──────────────┐
         ▼                                        ▼
┌──────────────────┐                     ┌──────────────────┐
│   Main Server    │ (Port 8081)         │   Chat Server    │ (Port 3001)
│ (Spring Boot 3.5)│                     │ (Node.js/Socket) │
└────────┬─────────┘                     └────────┬─────────┘
         │                                        │
         ├─── PostgreSQL 16 (Relational DB & Migrations)
         ├─── Redis 7 (Distributed Cache, Idempotency, Rate Limiting, Pub/Sub)
         └─── RabbitMQ (Async Event Bus & Mail Queues)
```

---

## 2. Configuration & Environment Guidelines
* **Strict Environment Property Binding:** Do NOT add default parameter fallbacks in `application.yaml` or `application.yml` files (e.g. use `${REDIS_PORT}`, not `${REDIS_PORT:6379}`). All configuration properties must be explicitly provided via environment variables (`.env`, Docker Compose, or Kubernetes) or supplied directly in test configuration classes (`@SpringBootTest(properties = {...})`).

---

## 3. Centralized Logging Architecture

All services output structured, rotated logs into a unified `logs/` directory mounted on the host machine.

### 2.1 Log Directory Hierarchy

```
logs/
├── server/
│   ├── server.log                 # Active server application logs (Logstash JSON)
│   ├── server-startup.log         # Bootstrap and initialization logs
│   ├── server.YYYY-MM-DD.i.log.gz # Rotated application log archives
│   └── server-startup.YYYY-MM-DD.i.log.gz
├── gateway/
│   ├── server.log                 # Active gateway application logs (Logstash JSON)
│   ├── server-startup.log         # Gateway bootstrap logs
│   └── *.log.gz                   # Rotated gateway log archives
├── chat-server/
│   ├── server.log                 # Active chat service logs (JSON formatted)
│   └── server-startup.log         # Chat server startup & connection logs
└── client/
    ├── access.log                 # Nginx HTTP request access log (standard format)
    └── error.log                  # Nginx error and diagnostic log
```

### 2.2 Logging Configuration Details

1. **Main Server (`server/src/main/resources/logback-spring.xml`)**:
   - **Production Profile (`prod`)**: Logs are emitted in Logstash JSON format to both console and rolling file appenders.
   - **Rolling Policies**:
     - `server.log`: Max 10MB per file, 7 days history, 100MB total size cap with gzip compression.
     - `server-startup.log`: Max 5MB per file, 3 days history, 20MB total size cap. Captures `SpringApplication`, `LoggingApplicationListener`, and `EcomApplication`.
   - **Development Profile (`dev`)**: Human-readable colorized console logging.

2. **Gateway (`gateway/src/main/resources/logback-spring.xml`)**:
   - Same production logback configuration as the main server, capturing `GatewayApplication` startup events.

3. **Chat Server (`server_chat/src/config/logger.js`)**:
   - Captures console output and appends structured JSON logs matching the Logstash schema to `logs/chat-server/server.log` and `server-startup.log`.

4. **Client Nginx (`client/nginx.main.conf` & `client/nginx.conf`)**:
   - Global Nginx configuration overrides default logging to prevent redundant logs.
   - Server block routes client traffic to `/var/log/nginx/access.log` (mounted to `logs/client/access.log`) and `/var/log/nginx/error.log` (mounted to `logs/client/error.log`).

---

## 3. Core Architecture Standards & Coding Rules

### 3.1 Date & Time Standard: `Instant`
- **Rule**: ALWAYS use `java.time.Instant` for entity timestamps (`createdAt`, `updatedAt`, `publishedAt`, etc.) and query filter boundaries.
- **Do NOT** use `LocalDateTime` because `LocalDateTime` lacks timezone awareness and causes serialization ambiguities across distributed systems.
- **Serialization**: `JavaTimeModule` is registered on the global `ObjectMapper` with `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS = false` to output standard UTC ISO-8601 format (`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`).

### 3.2 Centralized Cache Constants
- **Rule**: All Redis and Caffeine cache names MUST be referenced from `CacheConstants.java`.
- **Zero Magic Strings**:
  ```java
  @Cacheable(value = CacheConstants.CACHE_PRODUCTS, key = "#id")
  @CacheEvict(value = CacheConstants.CACHE_PRODUCTS, allEntries = true)
  ```

### 3.3 Idempotency Strategy
- All mutating financial and critical state endpoints support the `Idempotency-Key` header.
- Handled transparently by `IdempotencyAspect` + `IdempotencyService` with distributed Redis atomic locks (`SET NX PX`).
- Protects against duplicate order submissions and payment double-charges from network retries.

### 3.4 Rate Limiting & Proxy-Aware Filters
- **IP Extraction**: `RequestUtil.getClientIp()` extracts true client IPs behind reverse proxies and Docker gateways via `X-Forwarded-For` and `X-Real-IP`.
- **Filters**:
  - `LoggingFilter`: Masks sensitive credentials and assigns distributed MDC request tracing IDs (`requestId`).
  - `IpRateLimiterFilter`: Uses Bucket4j with Redis backing (and local Caffeine fallback in dev/test mode).
  - `AuthenticationFilter`: Parses and validates stateless JWT access tokens and populates Spring `SecurityContext`.

---

## 4. Database Migrations (Flyway)

Database schema evolutions are managed versioned migrations under:
`server/src/main/resources/db/migration/`

- `V1__init_schema.sql`: Full DDL creation for all tables, relational foreign keys, constraints, and indexes.
- `V2__initial_data.sql`: Seed data for administrative roles and system permissions.

### Migration Naming Convention
- `V<Version>__<Description>.sql` (e.g., `V3__add_discount_coupons.sql`)
- Always make migration scripts idempotent and avoid destructive schema updates on active production tables.

---

## 5. Docker Operations & Maintenance Guide

### 5.1 Quick Start Commands

| Action | Command |
| :--- | :--- |
| **Start all services** (detached) | `docker compose up -d` |
| **Rebuild & start all services** | `docker compose up --build -d` |
| **Stop all services** | `docker compose down` |
| **Stop and remove volumes** (resets DB/Redis) | `docker compose down -v` |
| **View running containers** | `docker compose ps` |
| **Check container resource stats** | `docker stats` |

### 5.2 Building & Managing Individual Services

```bash
# Rebuild a specific service without rebuilding everything
docker compose build server
docker compose build gateway
docker compose build chat-server
docker compose build client

# Rebuild and restart a single service without affecting other running containers
docker compose up -d --no-deps --build server
docker compose up -d --no-deps --build gateway
docker compose up -d --no-deps --build chat-server
docker compose up -d --no-deps --build client

# Restart a service
docker compose restart server
docker compose restart gateway
docker compose restart client
```

### 5.3 Viewing Logs

#### Via Docker Compose:
```bash
# Follow all logs
docker compose logs -f

# Follow specific service container logs
docker compose logs -f server
docker compose logs -f gateway
docker compose logs -f chat-server
docker compose logs -f client
```

#### Via Host Persistent Log Files:
```bash
# Server application logs
tail -f logs/server/server.log
tail -f logs/server/server-startup.log

# Gateway logs
tail -f logs/gateway/server.log
tail -f logs/gateway/server-startup.log

# Chat server logs
tail -f logs/chat-server/server.log

# Client Nginx HTTP access & error logs
tail -f logs/client/access.log
tail -f logs/client/error.log
```

### 5.4 Container Shell Access & Debugging

```bash
# Open interactive shell in main server container
docker compose exec server sh

# Open interactive shell in gateway container
docker compose exec gateway sh

# Open interactive shell in chat server container
docker compose exec chat-server sh

# Open interactive shell in client Nginx container
docker compose exec client sh

# Connect directly to PostgreSQL inside container
docker compose exec postgres psql -U ecom -d ecom

# Connect directly to Redis CLI inside container
docker compose exec redis redis-cli -a foobared
```

### 5.5 Maintenance & Troubleshooting

- **File Permissions on Host `logs/`**:
  If a container throws `Permission denied` when opening log files, grant appropriate write permissions to the host log directories:
  ```bash
  chmod 777 logs/server logs/gateway logs/chat-server logs/client
  ```
- **Prune Unused Docker Artifacts**:
  ```bash
  docker system prune -f
  docker builder prune -f
  ```

---

## 6. Local Development Setup (Without Docker)

### Backend (Server)
```bash
cd server
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Gateway
```bash
cd gateway
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Chat Server
```bash
cd server_chat
npm install
npm run dev
```

### Frontend (Client)
```bash
cd client
npm install
npm run dev
```

### Running Unit & Integration Tests
```bash
cd server
./gradlew test
```
