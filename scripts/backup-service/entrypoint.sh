#!/bin/sh

# Configuration from environment variables
# DB_HOST, DB_NAME, DB_USER, DB_PASS
# GDRIVE_SYNC_ENABLED (true/false)
# BACKUP_RETENTION_DAYS (e.g. 30)

BACKUP_DIR="/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/db_backup_$TIMESTAMP.sql.gz"
LOG_FILE="/var/log/backup.log"

echo "[$(date)] --- Starting Daily Backup ---" >> "$LOG_FILE"

# 1. Perform PostgreSQL Backup
PGPASSWORD="$DB_PASS" pg_dump -h "$DB_HOST" -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "[$(date)] Backup successful: $BACKUP_FILE" >> "$LOG_FILE"
    
    # 2. Optional Google Drive Sync
    if [ "$GDRIVE_SYNC_ENABLED" = "true" ]; then
        echo "[$(date)] Google Drive sync enabled. Uploading..." >> "$LOG_FILE"
        rclone copy "$BACKUP_FILE" gdrive:ecom-backups --config /config/rclone.conf
        if [ $? -eq 0 ]; then
            echo "[$(date)] Upload to Google Drive successful." >> "$LOG_FILE"
        else
            echo "[$(date)] Upload to Google Drive FAILED." >> "$LOG_FILE"
        fi
    fi
else
    echo "[$(date)] Backup FAILED." >> "$LOG_FILE"
    exit 1
fi

# 3. Weekly Cleanup (Only on Fridays)
DAY_OF_WEEK=$(date +%u) # 1=Mon, 5=Fri, 7=Sun
if [ "$DAY_OF_WEEK" -eq 5 ]; then
    echo "[$(date)] Friday detected. Running cleanup..." >> "$LOG_FILE"
    # Delete local files older than retention period
    find "$BACKUP_DIR" -type f -name "db_backup_*.sql.gz" -mtime +"${BACKUP_RETENTION_DAYS:-30}" -delete
    echo "[$(date)] Cleanup complete." >> "$LOG_FILE"
else
    echo "[$(date)] Today is not Friday (Day $DAY_OF_WEEK). Skipping cleanup." >> "$LOG_FILE"
fi

echo "[$(date)] --- Backup Process Finished ---" >> "$LOG_FILE"
