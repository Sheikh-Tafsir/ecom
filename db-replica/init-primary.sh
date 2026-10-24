#!/bin/bash
set -e

# Creates the replication user and configures pg_hba.conf for streaming replication.
# This script runs inside the primary Postgres container via docker-entrypoint-initdb.d.

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${REPLICATION_USER}') THEN
            CREATE ROLE ${REPLICATION_USER} WITH REPLICATION LOGIN PASSWORD '${REPLICATION_PASSWORD}';
            RAISE NOTICE 'Created replication user: ${REPLICATION_USER}';
        END IF;
    END
    \$\$;
EOSQL

# Allow replication connections from the Docker network (172.16.0.0/12 covers default Docker subnets)
echo "host replication ${REPLICATION_USER} 172.16.0.0/12 md5" >> "$PGDATA/pg_hba.conf"
echo "host replication ${REPLICATION_USER} 10.0.0.0/8 md5" >> "$PGDATA/pg_hba.conf"

echo "[init-primary] Replication user created and pg_hba.conf updated."
