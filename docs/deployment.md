# Deployment

## Docker Compose (Primary Method)

All services are orchestrated via `docker-compose.yml` at the repo root. The `nginx-proxy` service acts as the single entry point, routing traffic to the client and gateway. Optional replica overlay files (e.g., `docker-compose.replicas.yml`) can be layered on top for read-replica setups.

### Quick Commands

| Action                              | Command                                          |
|-------------------------------------|--------------------------------------------------|
| Start all services (detached)       | `docker compose up -d`                           |
| Rebuild all images + start          | `docker compose up --build -d`                   |
| Stop all services                   | `docker compose down`                            |
| Stop + delete volumes (resets DB)   | `docker compose down -v`                         |
| Rebuild one service, keep others    | `docker compose up -d --no-deps --build <name>`  |
| Restart one service                 | `docker compose restart <name>`                  |
| View running containers             | `docker compose ps`                              |
| Resource usage                      | `docker stats`                                   |

Service names: `server`, `gateway`, `chat-server`, `client`, `postgres`, `redis`, `rabbitmq`, `db-backup`, `nginx-proxy`, `smoke-test`, `health-monitor`.

### Service Start Order (enforced by `depends_on`)
```
postgres (healthy) ──┐
redis (healthy)    ──┼──▶ server (healthy) ──┐
rabbitmq (healthy) ──┘                       ├──▶ gateway (healthy) ──▶ client ──┐
                                             │                                   ├──▶ nginx-proxy
redis (healthy)    ──┬──▶ chat-server (healthy) ──┘                              │
postgres (healthy) ──┘                                     gateway (healthy) ──────┘
```

### Port Exposure

Docker Compose provides two mechanisms for making a container's port reachable. Only use `ports:` when host access is required:

| Directive | Reachable from host? | Reachable from other containers? | Use for |
|---|---|---|---|
| `ports: "0.0.0.0:80:80"` | ✅ Yes (all interfaces) | ✅ Yes | Public entry points (nginx-proxy) |
| `ports: "127.0.0.1:8080:8080"` | ✅ Localhost only | ✅ Yes | Dev/debug access (gateway) |
| `expose: "8081"` | ❌ No | ✅ Yes (same Docker network) | Internal services (server, chat-server, client) |

Containers on the same Docker network reach each other by **service name** — Docker's internal DNS resolves `server` to the container's IP. No port mapping is needed for inter-container communication.

| Service | Exposure | Binding |
|---|---|---|
| nginx-proxy | `ports: "0.0.0.0:${APP_PORT:-80}:80"` | Public — sole entry point |
| gateway | `ports: "127.0.0.1:${GATEWAY_PORT:-8080}:8080"` | Localhost only — dev/debug |
| server | `expose: "8081"` | Internal only |
| chat-server | `expose: "${CHAT_SERVER_PORT}"` | Internal only |
| client | `expose: "80"` | Internal only |
| postgres, redis, rabbitmq | (none) | Internal only — `data` network |

---

## Environment Variables

All env vars are in the root `.env` file and injected by Docker Compose. **No default fallbacks** in application config files.

### Global
| Variable                 | Example / Notes                           |
|--------------------------|-------------------------------------------|
| `DEPLOYMENT_ENVIRONMENT` | `prod` or `dev`                           |
| `APP_CACHE_STRATEGY`     | `redis` (prod) or `NONE` (CI/dev)         |
| `APP_PORT`               | `80` (default)                            |
| `GATEWAY_PORT`           | `8080`                                    |
| `MAIN_SERVER_PORT`       | `8081`                                    |
| `CHAT_SERVER_PORT`       | `3001`                                    |
| `CLIENT_PORT`            | `80` (prod), `5173` (dev)                 |
| `SERVER_URL`             | `http://localhost:8080`                   |
| `CLIENT_URL`             | `http://localhost`                        |

### Database (PostgreSQL)
| Variable          | Notes                                |
|-------------------|--------------------------------------|
| `DB_URL`          | JDBC URL: `jdbc:postgresql://postgres:5432/<db>` |
| `DB_USERNAME`     | Postgres user                        |
| `DB_PASSWORD`     | Postgres password                    |
| `DB_NAME`         | Database name                        |
| `DB_DRIVER_CLASS` | `org.postgresql.Driver`              |

### Redis
| Variable         | Notes                        |
|------------------|------------------------------|
| `REDIS_HOST`     | `redis` (Docker service name)|
| `REDIS_PORT`     | `6379`                       |
| `REDIS_PASSWORD` | Required; no anonymous Redis |

### RabbitMQ
| Variable      | Notes                          |
|---------------|--------------------------------|
| `MQ_HOST`     | `rabbitmq`                     |
| `MQ_PORT`     | `5672`                         |
| `MQ_USERNAME` | Default: `guest`               |
| `MQ_PASSWORD` | Default: `guest`               |

### JWT
| Variable               | Notes                                        |
|------------------------|----------------------------------------------|
| `ACCESS_TOKEN_SECRET`  | Minimum 32 chars hex string                  |
| `REFRESH_TOKEN_SECRET` | Separate secret for refresh tokens           |
| `REFRESH_TOKEN_NAME`   | Cookie name: `refreshToken`                  |

### Client Build Args (Vite — baked in at build time)
| Variable                     | Notes                                                              |
|------------------------------|--------------------------------------------------------------------|
| `VITE_API_PATH`              | `/api` (relative same-origin path; routed through nginx-proxy)     |
| `VITE_GOOGLE_LOGIN_CLIENT_ID`| Google OAuth client ID         |
| `VITE_SESSION_HINT_KEY`      | e.g., `ecom_session`           |
| `VITE_LOCAL_STORAGE_CART_KEY`| e.g., `visoredCart`            |
| `VITE_WEB_SOCKET_ON`         | `true` / `false`               |

### External Services
| Variable                | Service        | Notes                                  |
|-------------------------|----------------|----------------------------------------|
| `MAIL_SMTP_HOST`        | Email          | `smtp.gmail.com`                       |
| `MAIL_SMTP_PORT`        | Email          | `587`                                  |
| `MAIL_SMTP_USERNAME`    | Email          | Gmail address                          |
| `MAIL_SMTP_PASSWORD`    | Email          | Gmail app password (not account pwd)   |
| `CLOUDINARY_CLOUD_NAME` | Image CDN      | Cloudinary account                     |
| `CLOUDINARY_API_KEY`    | Image CDN      |                                        |
| `CLOUDINARY_API_SECRET` | Image CDN      |                                        |
| `CLOUDINARY_FOLDER`     | Image CDN      | Default: `ecom_uploads`                |
| `SUPABASE_URL`          | Image storage  | Supabase project URL                   |
| `SUPABASE_API_KEY`      | Image storage  | Service role key                       |
| `SUPABASE_BUCKET`       | Image storage  | Bucket name                            |
| `GOOGLE_LOGIN_CLIENT_ID`| OAuth          | Google OAuth 2.0 client ID             |
| `BKASH_BASE_URL`        | Payment        | Sandbox vs production URL              |
| `BKASH_APP_KEY`         | Payment        |                                        |
| `BKASH_APP_SECRET`      | Payment        |                                        |
| `BKASH_USERNAME`        | Payment        |                                        |
| `BKASH_PASSWORD`        | Payment        |                                        |

### Backup Service
| Variable                 | Notes                                          |
|--------------------------|------------------------------------------------|
| `BACKUP_DB_HOST`         | `postgres`                                     |
| `BACKUP_DB_NAME`         | Database name to back up                       |
| `BACKUP_RETENTION_DAYS`  | `7`                                            |
| `GDRIVE_SYNC_ENABLED`    | `true` / `false`                               |
| `BACKUP_ENCRYPTION_KEY`  | **Must be changed before production.** AES-256-CBC key. Generate: `openssl rand -base64 32` |

**Backup scope & restore**
- `db-backup` dumps the **main** database only (`BACKUP_DB_NAME`). The chat database (`postgres-chat`) is **not** backed up.
- Files are written to `./backups` on the same host; off-host copies exist only if `GDRIVE_SYNC_ENABLED=true` (rclone config in `db-backup/config`).
- `BACKUP_RETENTION_DAYS` is fixed to `7` in `docker-compose.yml`.
- Restore (from `db-backup/entrypoint.sh`):
  ```bash
  openssl enc -d -aes-256-cbc -pbkdf2 -k "$BACKUP_ENCRYPTION_KEY" \
    -in backups/db_backup_<TS>.sql.gz.enc | gunzip | psql -h <host> -U <user> <db>
  ```
  Test a restore into a scratch database periodically — an untested backup is not a backup.

### Persistence by Service

| Service | Volume | Survives `docker compose down`? | Survives `down -v`? |
|---|---|---|---|
| postgres | `postgres_data` | ✅ | ❌ |
| postgres-chat | `postgres_chat_data` | ✅ | ❌ (and not backed up) |
| redis | `redis_data` | ✅ (default RDB snapshots) | ❌ |
| rabbitmq | **none** | ❌ queues/messages lost on container recreate | ❌ |

### Read Replicas

See [docs/read-replicas.md](./read-replicas.md) for full setup instructions.

| Variable               | Notes                                        |
|------------------------|----------------------------------------------|
| `REPLICATION_USER`     | Dedicated PostgreSQL replication user         |
| `REPLICATION_PASSWORD` | Password for the replication user             |
| `REPLICA_PRIMARY_HOST` | Hostname of the primary PostgreSQL instance   |

---

## Logs

In `docker-compose.yml` the log directories are **named Docker volumes**, not host bind mounts. Read them with `docker compose exec` or `docker compose logs`. A `logs/` folder on the host is only populated when you run the services outside Docker, or with a local override that bind-mounts it.

| Service | Path inside container | Volume |
|---|---|---|
| server | `/app/logs/server/server.log`, `server-startup.log` | `server_logs` |
| gateway | `/app/logs/gateway/server.log`, `server-startup.log` | `gateway_logs` |
| chat-server | `/app/logs/chat-server/server.log` | `chat_server_logs` |
| client (Nginx) | `/var/log/nginx/access.log`, `error.log` | `client_logs` |

**Format:** Production logs are Logstash JSON (server, gateway, chat-server). Nginx uses standard combined log format.
**Rotation:** `server.log` — 10MB max, 7 days, 100MB total cap. `server-startup.log` — 5MB max, 3 days.

**Where logging is configured:**
- Main Server: `server/src/main/resources/logback-spring.xml` — `prod` profile writes Logstash JSON to console + rolling files (archives named `server.YYYY-MM-DD.i.log.gz`); `server-startup.log` captures `SpringApplication` / `EcomApplication` bootstrap only. `dev` profile is colorized console only.
- Gateway: `gateway/src/main/resources/logback-spring.xml` — same layout as the server.
- Chat Server: `server_chat/src/config/logger.js` (pino) — JSON in the Logstash schema; also re-routes stray `console.*` calls.
- Client Nginx: `client/nginx.main.conf` (global) + `client/nginx.conf` (server block).
- Docker's own `json-file` driver additionally keeps 3 × 10MB per container (`docker compose logs`).

```bash
# Container stdout (all services, incl. nginx-proxy)
docker compose logs -f --tail=200 server
docker compose logs -f --since=30m gateway chat-server

# Rolling log files inside the volumes
docker compose exec server  tail -f /app/logs/server/server.log
docker compose exec gateway tail -f /app/logs/gateway/server.log
docker compose exec chat-server tail -f /app/logs/chat-server/server.log
docker compose exec client  tail -f /var/log/nginx/access.log
```

---

## Debugging

```bash
# Shell into a container
docker compose exec server sh
docker compose exec gateway sh
docker compose exec chat-server sh
docker compose exec client sh

# Database access
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'

# Redis CLI
docker compose exec redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli'
```

---

## CI/CD Pipeline (GitHub Actions)

Defined in `.github/workflows/ci.yml`.

| Job               | Trigger                   | Action                                      |
|-------------------|---------------------------|---------------------------------------------|
| `server-build`    | push/PR                   | `./gradlew build` for `server` + `gateway`  |
| `client-build`    | push/PR                   | `npm ci && npm run build` for `client`       |
| `chat-server-build`| push/PR                  | `npm ci` for `server_chat`                  |
| `docker-build`    | after all 3 above pass    | `docker compose build` with minimal `.env`  |
| `deploy`          | `main`/`master` push only | Deployment trigger (customize per infra)    |

- CI uses a **minimal stub `.env`** with dummy secrets — enough for image builds only.
- JDK 21 (Temurin) for Spring services; Node 22 for JS services.

---

## Resource Limits (Production Docker)

| Service       | Memory Limit | CPU Limit |
|---------------|-------------|-----------|
| server        | 768M        | 1.5       |
| gateway       | 512M        | 1.0       |
| chat-server   | 512M        | 1.0       |
| client        | 256M        | 0.5       |
| postgres      | 512M        | 1.0       |
| redis         | 256M        | 0.5       |
| nginx-proxy   | 128M        | 0.5       |
