# PostgreSQL Read Replicas

Streaming replication setup with 2 read replicas. Works on a single machine (local/same VPS) and can be split to separate VPS machines with a one-line config change.

## Quick Start (Same Machine)

```bash
# First time: delete existing postgres volume so init script runs
docker compose down
docker volume rm ecom_postgres_data

# Start everything with replicas
docker compose -f docker-compose.yml -f docker-compose.replica.yml up -d
```

## Verify Replication

```bash
# Check replication status on the primary
docker compose exec postgres psql -U postgres -c "SELECT client_addr, state, sync_state FROM pg_stat_replication;"

# Should show 2 rows with state = 'streaming'

# Verify replicas are read-only
docker compose exec postgres-replica-1 psql -U postgres -c "SELECT pg_is_in_recovery();"
# Should return: t (true = read-only replica)

# Test: write on primary, read on replica
docker compose exec postgres psql -U postgres -c "CREATE TABLE replica_test (id int); INSERT INTO replica_test VALUES (1);"
docker compose exec postgres-replica-1 psql -U postgres -c "SELECT * FROM replica_test;"
# Should return: 1

# Verify replica rejects writes
docker compose exec postgres-replica-1 psql -U postgres -c "INSERT INTO replica_test VALUES (2);"
# Should fail: ERROR: cannot execute INSERT in a read-only transaction
```

## Split to Separate VPS

When you're ready to move replicas to their own machines:

**VPS 1 (Primary)** — expose Postgres port, everything else stays the same:
```yaml
# In docker-compose.yml, add to postgres service:
ports:
  - "5432:5432"
```

Then keep running the same command:
```bash
docker compose up -d
```

**VPS 2 (Replica)** — copy these files to the replica VPS:
```
docker-compose.replica-standalone.yml
db-replica/
.env   (copy and edit)
```

Edit `.env` on the replica VPS — set the primary's IP:
```bash
REPLICA_PRIMARY_HOST=142.93.45.67   # ← your primary VPS IP
REPLICATION_USER=replicator         # must match primary
REPLICATION_PASSWORD=...            # must match primary
DB_USERNAME=postgres                # must match primary
DB_NAME=postgres                    # must match primary
```

Then run:
```bash
docker compose -f docker-compose.replica-standalone.yml up -d
```

## Files

| File | Purpose |
|:-----|:--------|
| `docker-compose.replica.yml` | Overlay — runs primary + 2 replicas on the **same machine** |
| `docker-compose.replica-standalone.yml` | Standalone — runs a replica on a **separate VPS** |
| `db-replica/init-primary.sh` | Runs once on primary — creates replication user + pg_hba rules |
| `db-replica/entrypoint-replica.sh` | Replica entrypoint — clones primary on first run, resumes on restart |

## Env Vars

| Variable | Default | Description |
|:---------|:--------|:------------|
| `REPLICATION_USER` | — | Username for replication connections |
| `REPLICATION_PASSWORD` | — | Password for replication user |
| `REPLICA_PRIMARY_HOST` | `postgres` | Hostname/IP of primary. Change to VPS IP when splitting. |

## Running Without Replicas

Just use the normal command — replicas are opt-in:
```bash
docker compose up -d  # no replicas, same as before
```
