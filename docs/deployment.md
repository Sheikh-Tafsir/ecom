# Deployment

## Docker Compose (Primary Method)

All services are orchestrated via `docker-compose.yml` at the repo root.

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

Service names: `server`, `gateway`, `chat-server`, `client`, `postgres`, `redis`, `rabbitmq`, `db-backup`.

### Service Start Order (enforced by `depends_on`)
```
postgres (healthy) ──┐
redis (healthy)    ──┼──▶ server (healthy) ──┐
rabbitmq (healthy) ──┘                       ├──▶ gateway (healthy) ──▶ client
                                             │
redis (healthy)    ──┬──▶ chat-server (healthy) ──┘
postgres (healthy) ──┘
```

---

## Environment Variables

All env vars are in the root `.env` file and injected by Docker Compose. **No default fallbacks** in application config files.

### Global
| Variable                 | Example / Notes                           |
|--------------------------|-------------------------------------------|
| `DEPLOYMENT_ENVIRONMENT` | `prod` or `dev`                           |
| `APP_CACHE_STRATEGY`     | `redis` (prod) or `NONE` (CI/dev)         |
| `GATEWAY_PORT`           | `8080`                                    |
| `MAIN_SERVER_PORT`       | `8081`                                    |
| `CHAT_SERVER_PORT`       | `3001`                                    |
| `CLIENT_PORT`            | `80` (prod), `5173` (dev)                 |
| `SERVER_URL`             | `http://localhost:8080`                   |
| `CLIENT_URL`             | `http://localhost:5173`                   |

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
| Variable                     | Notes                          |
|------------------------------|--------------------------------|
| `VITE_API_PATH`              | `SERVER_URL`                   |
| `VITE_GOOGLE_LOGIN_CLIENT_ID`| Google OAuth client ID         |
| `VITE_SESSION_HINT_KEY`      | e.g., `ecom_session`           |
| `VITE_LOCAL_STORAGE_CART_KEY`| e.g., `visoredCart`            |
| `VITE_WEB_SOCKET_ON`         | `true` / `false`               |
| `VITE_SSE_ON`                | `true` / `false`               |

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

---

## Logs

All service logs are written to a unified `logs/` directory on the host.

| Service      | Log Files                                              |
|--------------|--------------------------------------------------------|
| server       | `logs/server/server.log`, `logs/server/server-startup.log` |
| gateway      | `logs/gateway/server.log`, `logs/gateway/server-startup.log` |
| chat-server  | `logs/chat-server/server.log`                          |
| client (Nginx)| `logs/client/access.log`, `logs/client/error.log`    |

**Format:** Production logs are Logstash JSON (server, gateway, chat-server). Nginx uses standard combined log format.
**Rotation:** `server.log` — 10MB max, 7 days, 100MB total cap. `server-startup.log` — 5MB max, 3 days.

```bash
# Live tail examples
tail -f logs/server/server.log
tail -f logs/gateway/server.log
tail -f logs/chat-server/server.log
tail -f logs/client/access.log

# Via Docker
docker compose logs -f server
docker compose logs -f gateway
```

If containers throw `Permission denied` on log files:
```bash
chmod 777 logs/server logs/gateway logs/chat-server logs/client
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
docker compose exec postgres psql -U ecom -d ecom

# Redis CLI
docker compose exec redis redis-cli -a foobared
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
