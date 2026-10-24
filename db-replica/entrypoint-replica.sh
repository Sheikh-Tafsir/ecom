#!/bin/bash
set -e

# Entrypoint for a Postgres streaming replica container.
# On first run: clones data from the primary via pg_basebackup.
# On subsequent runs: just starts Postgres (it resumes replication automatically).

PGDATA="/var/lib/postgresql/data"

if [ ! -s "$PGDATA/PG_VERSION" ]; then
    echo "[replica] No data found. Cloning from primary: ${PRIMARY_HOST}:${PRIMARY_PORT}..."

    # Wait for primary to be ready
    until pg_isready -h "$PRIMARY_HOST" -p "$PRIMARY_PORT" -U "$REPLICATION_USER"; do
        echo "[replica] Waiting for primary..."
        sleep 2
    done

    # Clone the primary's data directory
    PGPASSWORD="$REPLICATION_PASSWORD" pg_basebackup \
        -h "$PRIMARY_HOST" \
        -p "$PRIMARY_PORT" \
        -U "$REPLICATION_USER" \
        -D "$PGDATA" \
        --wal-method=stream \
        --checkpoint=fast \
        --write-recovery-conf \
        -R

    # -R flag creates standby.signal and sets primary_conninfo automatically
    echo "[replica] Base backup complete. Starting as read replica."
else
    echo "[replica] Existing data found. Resuming replication."
fi

# Hand off to the standard Postgres entrypoint
exec docker-entrypoint.sh postgres
