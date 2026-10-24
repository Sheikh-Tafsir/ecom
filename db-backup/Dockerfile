FROM alpine:3.19

# Install postgresql-client, rclone, and supercronic (a better crontab for Docker)
RUN apk add --no-cache postgresql16-client rclone curl bash

# Install supercronic for reliable cron in Docker
ENV SUPERCRONIC_URL=https://github.com/aptible/supercronic/releases/download/v0.2.25/supercronic-linux-amd64 \
    SUPERCRONIC=supercronic-linux-amd64 \
    SUPERCRONIC_SHA1SUM=642f4f5a2b67f3400b5ea71ff24f18c0a7d77d49

RUN curl -fsSLO "$SUPERCRONIC_URL" \
    && echo "${SUPERCRONIC_SHA1SUM}  ${SUPERCRONIC}" | sha1sum -c - \
    && chmod +x "$SUPERCRONIC" \
    && mv "$SUPERCRONIC" /usr/local/bin/supercronic

# Create backup and config directories
RUN mkdir -p /backups /config /scripts

# Copy the backup script
COPY entrypoint.sh /scripts/backup.sh
RUN chmod +x /scripts/backup.sh

# Create the crontab file (Run every day at 00:00)
RUN echo "0 0 * * * /scripts/backup.sh" > /etc/crontabs/backup-cron

# Start supercronic
ENTRYPOINT ["/usr/local/bin/supercronic", "/etc/crontabs/backup-cron"]
