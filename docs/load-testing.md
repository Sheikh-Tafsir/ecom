# Load Testing

How to run load tests against the platform using [k6](https://k6.io/). Test scripts live in `tests/load/`.

---

## Prerequisites

### Install k6

```bash
# Ubuntu / Debian
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 \
  --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D68
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6

# Or snap
snap install k6
```

### Test Users

Create dedicated load-test users in the dev database before running authenticated tests. These users should have:
- A verified account (OTP already confirmed)
- At least one existing chat conversation (for chat tests)
- Products in stock (for order tests)

---

## Test Scenarios

| Script | What It Tests | Auth Required | Default VUs |
|---|---|---|---|
| `tests/load/browse.js` | Product listing, detail, categories, banners | No | 30→50 |
| `tests/load/auth.js` | Login → verify → logout cycle | Yes (creates its own) | 3→5 |
| `tests/load/order.js` | Browse → place order (with idempotency key) | Yes | 3→10 |
| `tests/load/chat.js` | Chat list → messages → mark viewed | Yes | 5→10 |
| `tests/load/full-suite.js` | All above combined (realistic traffic mix) | Yes | Mixed |

All scripts import shared utilities from `tests/load/helpers.js`.

---

## Running Tests

### Quick Start — Single Scenario

```bash
# Browsing (no auth needed)
k6 run tests/load/browse.js

# Auth flow
k6 run -e TEST_EMAIL=loadtest@example.com -e TEST_PASS=LoadTest123! \
  tests/load/auth.js

# Order placement
k6 run -e TEST_EMAIL=loadtest@example.com -e TEST_PASS=LoadTest123! \
  tests/load/order.js

# Chat
k6 run -e TEST_EMAIL=loadtest@example.com -e TEST_PASS=LoadTest123! \
  tests/load/chat.js
```

### Full Suite — All Scenarios

```bash
k6 run -e TEST_EMAIL=loadtest@example.com -e TEST_PASS=LoadTest123! \
  tests/load/full-suite.js
```

The full suite runs all scenarios simultaneously with a realistic traffic distribution:

| Scenario | Weight | Executor | Peak VUs |
|---|---|---|---|
| Browsing | 60% | ramping-vus | 50 |
| Orders | 20% | ramping-vus | 10 |
| Chat | 10% | ramping-vus | 5 |
| Auth | 10% | constant-arrival-rate | 3 (18 req/min) |

### Custom Load Levels

Override VUs and duration from the command line:

```bash
# Light smoke test
k6 run --vus 5 --duration 30s tests/load/browse.js

# Heavy stress test
k6 run --vus 200 --duration 10m tests/load/browse.js
```

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `BASE_URL` | `https://localhost` | App URL (the Nginx proxy) |
| `TEST_EMAIL` | `loadtest@example.com` | Pre-existing user email |
| `TEST_PASS` | `LoadTest123!` | Password for the test user |
| `TEST_USERS` | — | Comma-separated user list (`u1@ex.com:p1,u2@ex.com:p2`) to pool across VUs and avoid per-user rate limits |
| `ADMIN_EMAIL` | — | Admin email (for admin endpoint tests) |
| `ADMIN_PASS` | — | Admin password |
| `CHAT_ID` | — | Specific chat ID (for chat tests; auto-detects if omitted) |

Pass via `-e`:

```bash
k6 run -e BASE_URL=https://staging.example.com \
       -e TEST_EMAIL=test@example.com \
       -e TEST_PASS=SecurePass123! \
       tests/load/full-suite.js
```

---

## Rate Limits to Be Aware Of

The Gateway enforces rate limits that will affect test results. These are **expected** behaviors, not bugs:

| Limit | Threshold | Affected Tests |
|---|---|---|
| Global | 2000 req/min, 100 concurrent | All (only at very high VU counts) |
| Per-IP | 100 req/min, 20 concurrent | All scenarios share one IP in local testing |
| Auth endpoints | 20 req/min, 5 concurrent per IP | `auth.js` — keep VUs low |
| Per-user | 300 req/min | `order.js`, `chat.js` |
| Chat messages | 20 msgs / 10s per user | `chat.js` (WebSocket; not hit by HTTP-only tests) |

The `auth.js` script is tuned to stay under the auth rate limit (3 iterations per 10s). If you see HTTP 429 responses, that's the rate limiter working correctly.

To avoid hitting per-user rate limits (300 req/min) when running tests with many VUs, supply multiple accounts via `TEST_USERS="u1@test.com:pass,u2@test.com:pass"`. The test scripts automatically distribute VUs across the token pool. Alternatively, increase the limits in `gateway/application.yml` → `rate-limit` section for capacity benchmarking.

---

## Thresholds (Pass/Fail Criteria)

Built-in thresholds in each script. k6 exits with code 99 if any threshold fails.

| Metric | Target | Notes |
|---|---|---|
| `http_req_duration` p95 | < 500ms | Overall |
| `http_req_duration` p95 for product listing | < 400ms | Core browsing experience |
| `http_req_duration` p95 for order placement | < 1000ms | Revenue-critical |
| `http_req_duration` p95 for login | < 800ms | Auth flow |
| `http_req_failed` rate | < 1–3% | Allows for some rate-limited 429s |

---

## Reading Results

k6 prints a summary table after each run:

```
     http_req_duration..........: avg=142ms  min=12ms  med=89ms  max=1.2s  p(90)=320ms  p(95)=450ms
     http_req_failed............: 0.42%  ✓ 12    ✗ 2834
     http_reqs..................: 2846   47.4/s
     iteration_duration.........: avg=2.1s   min=1.0s  med=1.8s  max=5.2s  p(90)=3.4s   p(95)=4.1s
```

Key metrics:
- **`http_req_duration` p95** — 95% of requests complete within this time. This is your latency SLO.
- **`http_req_failed`** — Percentage of non-2xx responses (excluding expected 429s in auth tests).
- **`http_reqs`** — Total throughput (requests per second).
- **`iteration_duration`** — Full user-flow time including sleeps.

### JSON Output for Dashboards

```bash
k6 run --out json=results.json tests/load/full-suite.js
```

### Grafana + InfluxDB (Real-Time Dashboard)

```bash
# Start InfluxDB
docker run -d --name influxdb -p 8086:8086 influxdb:1.8

# Run k6 with InfluxDB output
k6 run --out influxdb=http://localhost:8086/k6 tests/load/full-suite.js
```

Then import the [k6 Grafana dashboard](https://grafana.com/grafana/dashboards/2587) to visualize results in real time.

---

## Test Design Notes

### Why HTTP-only for Chat?

k6 has WebSocket support, but Socket.IO uses Engine.IO framing on top of WebSocket, which k6 cannot speak natively. The chat test exercises the HTTP REST endpoints (`GET /chats`, `GET /chats/:id`, `POST /chats/:id/view`) instead of WebSocket events.

For full Socket.IO load testing, use [Artillery](https://www.artillery.io/) with its `socketio-v3` engine:

```yaml
# artillery-chat.yml
config:
  target: "https://localhost"
  engines:
    socketio-v3:
      auth:
        token: "YOUR_ACCESS_TOKEN"
scenarios:
  - engine: socketio-v3
    flow:
      - emit: ["join-chat", { "chatId": "CHAT_ID" }]
      - think: 2
      - emit: ["send-message", { "chatId": "CHAT_ID", "content": "load test", "contentType": "text", "tempId": "temp-1" }]
      - think: 3
```

### Idempotency Keys

The `order.js` and `full-suite.js` scripts generate a unique UUID for each order via `uuidv4()`. This ensures each VU iteration creates a distinct order and doesn't hit the idempotency duplicate check.

### Shared Token

The `setup()` function logs in once and shares the token across all VUs. Since the access token is valid for 15 minutes and tests default to ~3 minutes, this works without refresh. For longer runs, refactor to include a token refresh step.
