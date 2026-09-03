#!/bin/bash

# Configuration
PROJECT_ROOT="/home/tafsirrubaiyat/webapp/ecom"
BACKUP_DIR="$PROJECT_ROOT/backups"
LOG_FILE="$PROJECT_ROOT/logs/backup.log"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/db_backup_$TIMESTAMP.sql.gz"

# Create backup directory and log directory if they don't exist
mkdir -p "$BACKUP_DIR"
mkdir -p "$PROJECT_ROOT/logs"

# Load environment variables from .env if it exists
if [ -f "$PROJECT_ROOT/.env" ]; then
    export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)
fi

DB_NAME=${BACKUP_DB_NAME:-${DB_NAME:-postgres}}
DB_USER=${DB_USERNAME:-postgres}

echo "[$(date)] Starting backup for database: $DB_NAME" >> "$LOG_FILE"

# Perform backup using docker compose exec
# We use -T to disable pseudo-TTY allocation (avoiding TTY issues in cron/non-interactive settings)
if docker compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T postgres pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_FILE"; then
    echo "[$(date)] Backup successful: $BACKUP_FILE" >> "$LOG_FILE"
else
    echo "[$(date)] Backup FAILED" >> "$LOG_FILE"
    exit 1
fi

# Keep only the last 7 days of backups
find "$BACKUP_DIR" -type f -name "db_backup_*.sql.gz" -mtime +7 -delete
echo "[$(date)] Old backups cleaned up" >> "$LOG_FILE"
