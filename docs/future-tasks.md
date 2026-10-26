# Future Tasks — Scaling & Infrastructure Roadmap

Infrastructure tasks that only become necessary when traffic grows or the system moves from a single VPS to several machines. None of them are blockers for the current single-host deployment.

Bugs, security fixes and missing features are **not** tracked here — see the [audit](../.context/2026-10-25-system-audit.md) for those.

---

## Phase 1 — Growth Preparation (1,000 → 10,000 Users)

When you start seeing database CPU climb or response times increase.

| Task | Priority | Effort | Impact | Notes |
|:-----|:---------|:-------|:-------|:------|
| Add CDN for static assets | High | Low | Faster page loads worldwide | Cloudflare free tier. Point DNS, enable proxying. React bundle (~2MB) served from edge locations instead of your VPS. |
| Activate PostgreSQL read replicas on **separate VPS** | High | Medium | Offloads read traffic to dedicated hardware | Same-VPS replicas don't scale (they share CPU/RAM/disk). The real benefit comes from splitting to separate machines. Config exists — see [read-replicas.md](./read-replicas.md) "Split to Separate VPS" section. **Note:** infrastructure only — the Main Server has no read/write routing yet, so it would need an `AbstractRoutingDataSource` keyed on `@Transactional(readOnly = true)` before replicas take any load. |
| Add PgBouncer connection pooling | High | Medium | Prevents connection exhaustion | PostgreSQL creates a process per connection — expensive. PgBouncer pools: 100 app connections → 20 DB connections. Critical when running multiple Main Server instances. |
| Run 2-3 Main Server instances | Medium | Medium | Higher throughput, redundancy | Main Server is stateless — just run more copies. Gateway already load-balances across them. Requires shared Redis cache (already have). **Prerequisite:** the `@Scheduled` jobs (order auto-reject, unverified-user and refresh-token cleanup) run on every instance — add ShedLock (or run them on one instance only) first. |
| Redis Sentinel for failover | Medium | Medium | Cache survives Redis restart | Sentinel auto-promotes a replica if Redis primary dies. Currently a Redis crash means cache miss storm hits PostgreSQL. |

---

## Phase 2 — Multi-Server (10,000 → 100,000 Users)

When a single VPS is no longer enough.

| Task | Priority | Effort | Impact | Notes |
|:-----|:---------|:-------|:-------|:------|
| Split services across multiple VPS | High | High | Dedicated resources per service | Separate data layer (PostgreSQL, Redis) from compute (Server, Chat). Use Docker Compose per VPS or transition to Swarm/K8s. |
| Managed database (RDS/Cloud SQL) | High | Medium | Automated backups, failover, scaling | Self-hosted PostgreSQL requires manual HA. Managed services handle replication, failover, and backups. |
| Scale Chat Server horizontally | Medium | Low | More concurrent WebSocket connections | Socket.IO Redis adapter already configured for multi-node. Add sticky sessions at the load balancer. |
| Separate Redis instances by concern | Medium | Medium | Isolation — cache eviction doesn't affect pub/sub | One Redis for caching (volatile-lru), one for pub/sub + presence (no eviction). Prevents cache pressure from evicting pub/sub keys. Security keys (revocation, OTP) should move to a `noeviction` instance earlier — see audit F-49. |
| Add distributed tracing (Jaeger/Zipkin) | Medium | High | End-to-end request visibility across services | Prometheus + Grafana moved to current tasks (`.context/tasks.md`). At multi-server scale, add Jaeger or Zipkin for distributed tracing across Gateway → Server → Chat to diagnose cross-service latency. |

---

## Phase 3 — Large Scale (100,000+ Users)

Architecture-level changes. Only pursue these when you have concrete evidence they're needed.

| Task | Priority | Effort | Impact | Notes |
|:-----|:---------|:-------|:-------|:------|
| Kubernetes migration | High | Very High | Auto-scaling, self-healing, rolling deploys | Only justified with 10+ services or when manual scaling becomes unsustainable. Use managed K8s (EKS/GKE/AKS). |
| Database sharding or Aurora/CockroachDB | High | Very High | Beyond single-node PostgreSQL limits | Sharding is extremely complex (no cross-shard joins). Prefer managed distributed databases over manual sharding. |
| Dedicated WebSocket gateway | Medium | High | Socket connections don't compete with REST for Gateway resources | Separate Gateway instance (or different service) for `/socket.io/**` traffic. Useful when chat traffic dwarfs REST traffic. |
| API response caching at CDN | Medium | Medium | Public endpoints (product listings) served from edge | Cache `GET /api/products` responses at CDN with short TTL. Reduces load on Gateway + Server for unauthenticated traffic. |
| Blue-green or canary deployments | Medium | High | Zero-downtime deploys, gradual rollouts | Route 5% of traffic to new version → monitor → promote. Requires load balancer or service mesh support. |

---

## Decision Framework — When to Invest

> Don't optimize for a problem you don't have yet.

```
Is the database CPU > 70%?
  → Yes → Read replicas, then PgBouncer, then indexes
  → No  → Don't touch the database

Are response times > 500ms for API calls?
  → Yes → Check: is it DB? Add indexes/cache. Is it network? Add CDN.
  → No  → Don't add infrastructure

Are you deploying more than once a week and afraid of downtime?
  → Yes → Blue-green deployments
  → No  → Manual deploys are fine

Do you have > 5 services and manual scaling is painful?
  → Yes → Consider Kubernetes
  → No  → Docker Compose is fine
```
