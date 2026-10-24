# Hot-Reload Configuration Guide

Change rate limits, business rules, and feature flags on a live server **without restarting, rebuilding, or redeploying**.

---

## How It Works

```
┌──────────────────────────────────────────────────────────┐
│  VPS Host                                                │
│                                                          │
│  config/server-overrides.properties  ← you edit this     │
│         │                                                │
│         │  (bind-mount, read-only)                       │
│         ▼                                                │
│  ┌─────────────────────┐     POST /actuator/refresh      │
│  │  Spring Boot Server  │ ◄──────────────────────────    │
│  │                      │                                │
│  │  @RefreshScope beans │ ← recreated with new values    │
│  │  (rate limiters,     │                                │
│  │   validators, etc.)  │                                │
│  └─────────────────────┘                                 │
└──────────────────────────────────────────────────────────┘
```

1. Override files live on the VPS host filesystem — **not in the codebase**
2. They're bind-mounted into the Docker containers
3. When you edit a file, the container sees the new content immediately
4. `POST /actuator/refresh` tells Spring to re-read the config and recreate all `@RefreshScope` beans
5. New values take effect instantly — zero downtime

---

## Step-by-Step

### 1. SSH into your VPS

```bash
ssh user@your-vps
cd /path/to/ecom    # where docker-compose.yml lives
```

### 2. Edit the override file

**For the Main Server:**
```bash
nano config/server-overrides.properties
```

**For the Gateway:**
```bash
nano config/gateway-overrides.properties
```

### 3. Trigger the refresh

```bash
curl -X POST http://localhost:8080/actuator/refresh \
  -H "X-Internal-Service-Key: $INTERNAL_SERVICE_KEY" \
  -H "Content-Type: application/json"
```

The response lists which properties changed:
```json
["rate-limit.ip.rate", "rate-limit.ip.concurrency"]
```

An empty `[]` means nothing actually changed.

---

## What Can You Change?

### Server Overrides (`config/server-overrides.properties`)

| Property | Default | What It Controls |
|:---------|:--------|:-----------------|
| `rate-limit.global.rate` | 2000 | Max requests/window across all users |
| `rate-limit.global.concurrency` | 100 | Max concurrent requests globally |
| `rate-limit.ip.rate` | 100 | Max requests/window per IP |
| `rate-limit.ip.concurrency` | 20 | Max concurrent requests per IP |
| `rate-limit.auth-ip.rate` | 20 | Max auth attempts/window per IP |
| `rate-limit.auth-ip.concurrency` | 5 | Max concurrent auth attempts per IP |
| `rate-limit.user.rate` | 300 | Max requests/window per authenticated user |
| `rate-limit.user.concurrency` | 15 | Max concurrent requests per user |
| `rate-limit.window-seconds` | 60 | Rate limit window duration |
| `rate-limit.otp.rate` | 1 | Max OTP requests/window |
| `rate-limit.otp.window-seconds` | 60 | OTP rate limit window |
| `rate-limit.otp.max-tries` | 3 | Max OTP verification attempts before lockout |
| `app.product.max-image-count` | 5 | Max images per product |
| `app.cms.max-total-banners` | 20 | Max total banners in the system |
| `app.cms.max-active-banners` | 5 | Max simultaneously active banners |
| `app.cms.max-total-faqs` | 10 | Max total FAQs |
| `app.upload.max-file-size-bytes` | 5242880 | Max upload file size (5MB) |

### Gateway Overrides (`config/gateway-overrides.properties`)

| Property | Default | What It Controls |
|:---------|:--------|:-----------------|
| `rate-limit.global.rate` | 2000 | Gateway-level global rate limit |
| `rate-limit.global.concurrency` | 100 | Gateway-level global concurrency |
| `rate-limit.ip.rate` | 100 | Gateway-level per-IP rate limit |
| `rate-limit.ip.concurrency` | 20 | Gateway-level per-IP concurrency |
| `rate-limit.auth-ip.rate` | 20 | Gateway-level auth rate limit |
| `rate-limit.auth-ip.concurrency` | 5 | Gateway-level auth concurrency |
| `rate-limit.user.rate` | 300 | Gateway-level per-user rate limit |
| `rate-limit.user.concurrency` | 15 | Gateway-level per-user concurrency |
| `rate-limit.window-seconds` | 60 | Gateway rate limit window |

> **Note:** The gateway and server have **independent** rate limiters. The gateway rate limiter runs first (pre-routing), the server rate limiter runs second (post-authentication). Both should be tuned together.

---

## Common Scenarios

### Bot Attack — Tighten IP rate limits immediately

```bash
# Edit the file
sed -i 's/rate-limit.ip.rate=100/rate-limit.ip.rate=20/' config/server-overrides.properties
sed -i 's/rate-limit.ip.rate=100/rate-limit.ip.rate=20/' config/gateway-overrides.properties

# Apply to both services
curl -s -X POST http://localhost:8080/actuator/refresh \
  -H "X-Internal-Service-Key: $INTERNAL_SERVICE_KEY" \
  -H "Content-Type: application/json"
```

### OTP Abuse — Reduce max tries

```bash
sed -i 's/rate-limit.otp.max-tries=3/rate-limit.otp.max-tries=1/' config/server-overrides.properties

curl -s -X POST http://localhost:8080/actuator/refresh \
  -H "X-Internal-Service-Key: $INTERNAL_SERVICE_KEY" \
  -H "Content-Type: application/json"
```

### Flash Sale — Allow more product images

```bash
sed -i 's/app.product.max-image-count=5/app.product.max-image-count=10/' config/server-overrides.properties

curl -s -X POST http://localhost:8080/actuator/refresh \
  -H "X-Internal-Service-Key: $INTERNAL_SERVICE_KEY" \
  -H "Content-Type: application/json"
```

### Revert to Defaults — Delete all overrides

```bash
# Clear overrides (defaults from application.yml will be used)
echo "" > config/server-overrides.properties

curl -s -X POST http://localhost:8080/actuator/refresh \
  -H "X-Internal-Service-Key: $INTERNAL_SERVICE_KEY" \
  -H "Content-Type: application/json"
```

---

## Security

The `/actuator/refresh` endpoint is protected by **three layers**:

1. **Port binding** — The gateway listens on `127.0.0.1` only. No external access.
2. **Service key** — Requires `X-Internal-Service-Key` header with the correct secret. Without it → `401 Unauthorized`.
3. **Defense in depth** — The Main Server also validates the same key independently, even if the gateway is bypassed.

---

## Troubleshooting

| Problem | Cause | Fix |
|:--------|:------|:----|
| `401 Unauthorized` | Missing or wrong service key | Check `$INTERNAL_SERVICE_KEY` matches `.env` |
| `[]` empty response | No properties actually changed | Verify the file was saved and the property name is spelled correctly |
| Change not taking effect | Bean not annotated with `@RefreshScope` | The property must be read by a `@RefreshScope` bean — check the table above for supported properties |
| `Connection refused` | Gateway not running or port wrong | Check `docker compose ps` and `$GATEWAY_PORT` |
