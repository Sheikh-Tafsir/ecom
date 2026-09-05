#!/bin/bash
set -eo pipefail

# Configuration
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKUP_DIR="$PROJECT_ROOT/backups"
LOG_FILE="$PROJECT_ROOT/logs/backup.log"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/db_backup_$TIMESTAMP.sql.gz"

# Create backup directory and log directory if they don't exist
mkdir -p "$BACKUP_DIR"
mkdir -p "$PROJECT_ROOT/logs"

# Load environment variables from .env if it exists
if [ -f "$PROJECT_ROOT/.env" ]; then
    set -a
    # shellcheck source=/dev/null
    . "$PROJECT_ROOT/.env"
    set +a
fi

DB_NAME=${BACKUP_DB_NAME:-${DB_NAME:-postgres}}
DB_USER=${DB_USERNAME:-postgres}

echo "[$(date)] Starting backup for database: $DB_NAME" >> "$LOG_FILE"

# Perform backup using docker compose exec
# We use -T to disable pseudo-TTY allocation (avoiding TTY issues in cron/non-interactive settings)
if docker compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T postgres pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_FILE"; then
    # Validate backup is not empty (pg_dump can succeed with empty output on error)
    if [ ! -s "$BACKUP_FILE" ]; then
        echo "[$(date)] Backup FAILED: output file is empty" >> "$LOG_FILE"
        rm -f "$BACKUP_FILE"
        exit 1
    fi
    echo "[$(date)] Backup successful: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))" >> "$LOG_FILE"
else
    echo "[$(date)] Backup FAILED" >> "$LOG_FILE"
    rm -f "$BACKUP_FILE"
    exit 1
fi

# Keep only the last 7 days of backups
find "$BACKUP_DIR" -type f -name "db_backup_*.sql.gz" -mtime +7 -delete
echo "[$(date)] Old backups cleaned up" >> "$LOG_FILE"
