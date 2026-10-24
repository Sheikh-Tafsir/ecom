# Database Backup Service with Google Drive Sync

This service automates daily backups, weekly cleanup, and optional Google Drive synchronization.

## Features
- **Daily Backup**: Runs at `00:00` every day.
- **Weekly Cleanup**: Runs only on **Fridays** to delete local files older than `BACKUP_RETENTION_DAYS`.
- **Google Drive Sync**: Optional upload to Google Drive after backup.

## Configuration

### 1. Enable Sync
In your `.env` file (or `docker-compose.yml`), set:
```env
GDRIVE_SYNC_ENABLED=true
```

### 2. Google Drive Setup (rclone)
To use the Google Drive sync, you must provide a valid `rclone.conf` file.

1. Install `rclone` on your local machine.
2. Run `rclone config` and create a remote named `gdrive`.
3. Follow the steps to authorize your Google account.
4. Copy the generated `rclone.conf` content (usually at `~/.config/rclone/rclone.conf`) to `scripts/backup-service/config/rclone.conf`.

## Files
- `entrypoint.sh`: The logic for backup, cleanup, and sync.
- `Dockerfile`: Builds the specialized container with `postgresql-client`, `rclone`, and `supercronic`.
- `config/rclone.conf`: Your Google Drive credentials (ignored by git).
