# Operations Runbook

Step-by-step procedures for running the Docker Compose deployment and handling incidents. For configuration and env vars see [deployment.md](./deployment.md). For how the parts fit together see [architecture.md](./architecture.md).

All commands run from the repo root on the host. Commands that need credentials read them from the container's own environment, so no secret is typed on the command line or stored in shell history.

---

## 1. Everyday operations

| Task | Command |
|---|---|
| Status of all containers + health | `docker compose ps` |
| Resource usage | `docker stats --no-stream` |
| Start everything | `docker compose up -d` |
| Deploy a change to one service | `docker compose up -d --no-deps --build <service>` |
| Restart one service | `docker compose restart <service>` |
| Follow logs | `docker compose logs -f --tail=200 <service>` |
| Run the smoke test once | `docker compose run --rm smoke-test` |
| Open a DB shell | `docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'` |
| Open a Redis shell | `docker compose exec redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli'` |
| Free disk space | `docker image prune -f && docker builder prune -f` (do **not** use `docker volume prune`; it deletes the databases) |

**Health endpoints** (from inside the Docker network, e.g. `docker compose exec gateway wget -qO- …`):

| Service | URL |
|---|---|
| Gateway | `http://gateway:8080/actuator/health` |
| Main Server | `http://server:8081/actuator/health` |
| Chat Server | `http://chat-server:3001/health` |
| Client | `http://client:80/` |

`health-monitor` runs `scripts/smoke-test.sh` every `HEALTH_CHECK_INTERVAL` seconds (default 300). It posts failures and recoveries to `ALERT_WEBHOOK_URL` (Discord/Slack format), at most one alert per 15 min.

---

## 2. Deploying

1. `git pull` on the host.
2. Check the pending migrations: `ls server/src/main/resources/db/migration/`. Flyway applies new ones when `server` starts, and **there is no automatic rollback**. Take a backup first (§6.1) whenever a migration is included.
3. `docker compose build` (to catch build failures before anything is stopped).
4. `docker compose up -d`.
5. `docker compose ps`: every service should become `healthy` within about 1–2 minutes.
6. `docker compose run --rm smoke-test`.
7. Watch `docker compose logs -f server gateway` for 5 minutes for `ERROR` lines.

**To roll back:** check out the previous commit and run `docker compose up -d --build`. This is only safe if the new migrations were backward-compatible. Otherwise restore the backup taken in step 2 (§6.2).

---

## 3. Payment incidents (bKash)

> Background: the callback flow and its known gaps are described in [architecture.md → Prepaid Checkout](./architecture.md#prepaid-checkout-bkash) and audit F-01–F-03. Until those are fixed, **check payments by hand daily**.

### 3.1 Daily reconciliation query

```bash
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
-- Payments not completed after 30 min, or orders whose state disagrees with their payments
SELECT p.created_at, p.status AS payment_status, p.payment_intent_id, p.transaction_id,
       p.amount, o.id AS order_id, o.status AS order_status, o.paid
FROM payments p JOIN orders o ON o.id = p.order_id
WHERE (p.status = 'PENDING' AND p.created_at < now() - interval '30 minutes')
   OR (p.status = 'FAILED'    AND o.status IN ('PENDING','REJECTED','CANCELLED'))
   OR (p.status = 'COMPLETED' AND o.status IN ('PENDING','REJECTED','CANCELLED'))
ORDER BY p.created_at DESC;

-- Orders with more than one non-failed payment (possible double charge)
SELECT order_id, count(*) FROM payments
WHERE status <> 'FAILED' GROUP BY order_id HAVING count(*) > 1;
SQL
```

### 3.2 "I was charged but my order failed / is still pending"

1. Find the order and its payments:
   ```sql
   SELECT id, status, paid, total_price, created_at FROM orders WHERE id = '<orderId>';
   SELECT id, payment_intent_id, transaction_id, amount, status, created_at
     FROM payments WHERE order_id = '<orderId>' ORDER BY created_at;
   ```
2. Ask bKash for the real status. An admin token is required: log in as an admin in the browser, then copy the access token from the network tab.
   ```bash
   curl -sk -H "Authorization: Bearer <admin-access-token>" \
     https://<your-domain>/api/payment/query/<payment_intent_id>
   ```
   Look at `transactionStatus` (`Completed`, `Initiated`, …) and `trxID`.
3. Decide based on what bKash reports:

| bKash says | Local state | Action |
|---|---|---|
| `Completed` | payment FAILED or PENDING, order PENDING | If stock is available, move the order to **Accepted** in the admin UI. Then correct the payment row: `UPDATE payments SET status='COMPLETED', transaction_id='<trxID>' WHERE id='<paymentId>';` and `UPDATE orders SET paid=true WHERE id='<orderId>';` |
| `Completed` | order REJECTED or CANCELLED (stock already released) | Refund (§3.3), or re-create the order manually if the customer still wants it |
| `Completed` twice for the same order | — | Refund the duplicate (§3.3) |
| Not completed / expired | payment PENDING | `UPDATE payments SET status='FAILED' WHERE id='<paymentId>';` — the customer was not charged |

4. Record what you did and why in `order_history`:
   ```sql
   INSERT INTO order_history (id, order_id, from_status, to_status, comment, created_at)
   VALUES (gen_random_uuid(), '<orderId>', '<old>', '<new>', 'Manual reconciliation: <reason>', now());
   ```

### 3.3 Refund

The admin-only endpoint passes the request straight to bKash. It does **not** check the amount or update any local state, so double-check every value:
```bash
curl -sk -X POST -H "Authorization: Bearer <admin-access-token>" \
  "https://<your-domain>/api/payment/refund?paymentID=<payment_intent_id>&trxID=<trxID>&amount=<amount>&reason=<text>"
```
Then set the order to **Refunded** in the admin UI (if the transition is allowed) and add an `order_history` note.

### 3.4 Customers are redirected to a 401/404 page after paying on bKash
The callback URL is misconfigured (audit F-03). It must be `${SERVER_URL}/api/payment/callback`. Fix `BKASH_CALLBACK_URL` in `docker-compose.yml`, then `docker compose up -d server`. Payments made while the URL was wrong were never executed; bKash voids them. Their payment rows stay PENDING and can be marked FAILED as in §3.2.

---

## 4. Stock incidents

### 4.1 Products show "out of stock" but the warehouse has units / phantom reservations
Old PENDING orders hold reserved stock. The nightly auto-reject job is currently broken (audit F-06).
```sql
-- Stale pending orders and what they reserve
SELECT o.id, o.created_at, oi.product_id, oi.quantity
FROM orders o JOIN order_items oi ON oi.order_id = o.id
WHERE o.status = 'PENDING' AND o.created_at < now() - interval '2 days';
```
Reject them through the admin UI (Orders → status **Rejected**). That path restores `products.quantity` correctly. Do not delete orders in SQL.

### 4.2 "Insufficient stock" when accepting an order that looks in stock
The two ledgers have drifted (audit F-04). Compare them:
```sql
SELECT p.id, p.name, p.quantity AS sellable,
       COALESCE(SUM(si.remaining),0) AS in_batches,
       (SELECT COALESCE(SUM(oi.quantity),0) FROM order_items oi JOIN orders o ON o.id=oi.order_id
         WHERE oi.product_id=p.id AND o.status='PENDING') AS reserved_pending
FROM products p LEFT JOIN stock_items si ON si.product_id = p.id
GROUP BY p.id HAVING p.quantity + (SELECT COALESCE(SUM(oi.quantity),0) FROM order_items oi
       JOIN orders o ON o.id=oi.order_id WHERE oi.product_id=p.id AND o.status='PENDING')
       <> COALESCE(SUM(si.remaining),0);
```
`sellable + reserved_pending` should equal `in_batches`. To fix a product, add a stock batch for the missing units in the admin UI (Stocks → Create), or adjust the existing batch item. Count the physical stock first.

---

## 5. Service outages

### 5.1 Redis down
**Symptoms:** API returns `503` (token revocation checks fail closed by design), chat sockets can't connect, presence disappears.
1. `docker compose ps redis` and `docker compose logs --tail=100 redis`.
2. Memory: `docker compose exec redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli info memory' | grep used_memory_human`. The limit is 256 MB.
3. `docker compose restart redis`. The catalog cache refills from PostgreSQL automatically.
4. After recovery: users may need to log in again if revocation keys were lost, and chat clients reconnect by themselves.

### 5.2 PostgreSQL (main) down
**Symptoms:** server health fails, gateway returns 503 or the circuit breaker opens, and every API call fails.
1. `docker compose logs --tail=200 postgres`. Common causes are a full disk (`df -h`) and the memory limit (512 MB, `docker stats`).
2. `docker compose restart postgres`, then `docker compose restart server` if the server doesn't recover its pool within a minute.
3. If data is corrupted, restore (§6.2).

### 5.3 Chat server crash-looping
**Symptoms:** `docker compose ps chat-server` shows restarts, chat and notifications are down, and the REST shop still works.
1. `docker compose logs --tail=300 chat-server`. Look for `Unhandled` / `TypeError` right before the exit.
2. A malformed socket event from a client can currently kill the process (audit F-11). If restarts keep coming in bursts, find the `user` in the log lines just before each crash and ban that account in the admin UI. Banning revokes the user's tokens.
3. If `postgres-chat` is down: `docker compose restart postgres-chat chat-server`.

### 5.4 Gateway returns 429 to everyone
Every request is keyed on the nginx container's IP when `TRUSTED_PROXY_IPS` is empty (audit F-10), so one busy client can exhaust the shared bucket. Short term: `docker compose restart gateway` (in-memory buckets reset) or flush the rate-limit keys in Redis. Long term: fix as described in F-10.

### 5.5 Uploads fail with 413
nginx's default body limit is 1 MB (audit F-25). Add `client_max_body_size 30m;` to the 443 server block in `nginx-proxy.conf`, then `docker compose restart nginx-proxy`.

### 5.6 TLS certificate expired / nginx won't start
See [ssl.md](./ssl.md). Replace `certs/cert.pem` and `certs/key.pem`, then `docker compose restart nginx-proxy`.

---

## 6. Backups & restore

### 6.1 Take a backup now
```bash
docker compose exec db-backup /scripts/backup.sh   # same script the daily cron runs
docker compose exec db-backup tail -5 /var/log/backup.log
ls -lh backups/ | tail -3                           # newest db_backup_<TS>.sql.gz.enc
```
Only the **main** DB is covered. To back up the chat DB by hand:
```bash
docker compose exec -T postgres-chat sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' | gzip > backups/chat_$(date +%Y%m%d_%H%M%S).sql.gz
```

### 6.2 Restore the main DB
1. Stop the writers: `docker compose stop server chat-server gateway`.
2. Restore into a **scratch database first** to verify the file:
   ```bash
   docker compose exec -T postgres sh -c 'createdb -U "$POSTGRES_USER" restore_check'
   openssl enc -d -aes-256-cbc -pbkdf2 -pass env:BACKUP_ENCRYPTION_KEY \
     -in backups/db_backup_<TS>.sql.gz.enc | gunzip | \
     docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d restore_check -q'
   ```
   (Export `BACKUP_ENCRYPTION_KEY` in your shell from `.env` first, e.g. `set -a; . ./.env; set +a`.)
3. Check the scratch DB: `SELECT count(*) FROM orders;`, plus the latest `created_at`.
4. Replace the real database: drop and recreate it, or rename `restore_check` to it while no connections are open. Then `docker compose up -d`.
5. Note how much data was lost (the last backup time) and run the payment reconciliation (§3.1).

**Restore drill:** repeat steps 2–3 once a quarter, and record the date and the duration here.

| Date | Backup used | Restore time | Result |
|---|---|---|---|
| — | — | — | not yet performed |

---

## 7. Security incidents

### 7.1 Rotate secrets (suspected leak, or a staff member leaves)
1. Generate new values: `openssl rand -hex 32` for each of `ACCESS_TOKEN_SECRET`, `REFRESH_TOKEN_SECRET` and `INTERNAL_SERVICE_KEY`.
2. Update `.env`. Rotate the provider-side credentials too (bKash, Cloudinary, SMTP, Google) if they may have leaked.
3. `docker compose up -d --force-recreate gateway server chat-server`.
4. Effect: every user is logged out (old tokens fail signature checks). This is expected.
5. Rebuild and redeploy any image that may contain an old `.env` (chat-server, audit F-15).

### 7.2 Compromised account
In the admin UI, set the user to **Banned**. This revokes the user's tokens; gateway and chat honour the change within about 15 min (audit F-16). For a compromised admin, also rotate `ACCESS_TOKEN_SECRET` (§7.1) to cut off any tokens it minted.

### 7.3 Change the seeded super-admin password
`V3__initial_data.sql` seeds a super admin with a fixed password hash that is committed to git. In every new environment, log in and change that password immediately, or replace the account (audit F-54).

---

## 8. Scheduled jobs (what runs when)

| Job | Schedule (server time) | Effect | Failure symptom |
|---|---|---|---|
| `OrderRejectScheduler` | daily 00:00 | Notify admins about orders pending more than 1 day; reject orders pending more than 2 days and release their stock | Stale PENDING orders pile up (§4.1) |
| `DeleteNotVerifiedUserScheduler` | daily 00:00 | Hard-delete unverified accounts older than 24 h | — |
| `DeleteUserRefreshTokenScheduler` | daily 00:00 | Delete refresh tokens revoked more than 1 day ago | `user_refresh_tokens` grows |
| `db-backup` (supercronic) | daily 00:00 (container time, UTC) | Encrypted dump of the main DB, 7-day retention | No new files in `backups/`; check `docker compose exec db-backup cat /var/log/backup.log` |
| `health-monitor` | every `HEALTH_CHECK_INTERVAL` s | Smoke test + webhook alert | No alerts despite an outage → check `docker compose logs health-monitor` |
