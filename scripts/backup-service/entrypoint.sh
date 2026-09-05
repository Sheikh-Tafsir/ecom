#!/bin/bash
set -eo pipefail

# Configuration from environment variables
# DB_HOST, DB_NAME, DB_USER, DB_PASS
# GDRIVE_SYNC_ENABLED (true/false)
# BACKUP_RETENTION_DAYS (e.g. 30)

BACKUP_DIR="/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/db_backup_$TIMESTAMP.sql.gz"
LOG_FILE="/var/log/backup.log"

echo "[$(date)] --- Starting Daily Backup ---" >> "$LOG_FILE"

# 1. Set up .pgpass for secure credential handling (avoids exposing password via env/proc)
PGPASS_FILE="$HOME/.pgpass"
echo "$DB_HOST:5432:$DB_NAME:$DB_USER:$DB_PASS" > "$PGPASS_FILE"
chmod 0600 "$PGPASS_FILE"

# 2. Perform PostgreSQL Backup with pipefail
if pg_dump -h "$DB_HOST" -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_FILE"; then
    # Validate backup is not empty (pg_dump can succeed with empty output on error)
    if [ ! -s "$BACKUP_FILE" ]; then
        echo "[$(date)] Backup FAILED: output file is empty" >> "$LOG_FILE"
        rm -f "$BACKUP_FILE"
        exit 1
    fi
    echo "[$(date)] Backup successful: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))" >> "$LOG_FILE"
    
    # 3. Optional Google Drive Sync
    if [ "$GDRIVE_SYNC_ENABLED" = "true" ]; then
        echo "[$(date)] Google Drive sync enabled. Uploading..." >> "$LOG_FILE"
        if rclone copy "$BACKUP_FILE" gdrive:ecom-backups --config /config/rclone.conf; then
            echo "[$(date)] Upload to Google Drive successful." >> "$LOG_FILE"
        else
            echo "[$(date)] Upload to Google Drive FAILED." >> "$LOG_FILE"
        fi
    fi
else
    echo "[$(date)] Backup FAILED." >> "$LOG_FILE"
    rm -f "$BACKUP_FILE"
    exit 1
fi

# 4. Cleanup old backups according to retention policy
echo "[$(date)] Running retention cleanup..." >> "$LOG_FILE"
find "$BACKUP_DIR" -type f -name "db_backup_*.sql.gz" -mtime +"${BACKUP_RETENTION_DAYS:-30}" -delete
echo "[$(date)] Cleanup complete." >> "$LOG_FILE"

echo "[$(date)] --- Backup Process Finished ---" >> "$LOG_FILE"
