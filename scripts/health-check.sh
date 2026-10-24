#!/bin/sh
# ==============================================================================
# Periodic Health Monitor — Runs smoke-test.sh in a loop with alerting
# ==============================================================================
# Env vars:
#   HEALTH_CHECK_INTERVAL  — seconds between checks (default: 300 = 5 min)
#   ALERT_WEBHOOK_URL      — Discord/Slack-compatible webhook for failure alerts (optional)
#   All smoke-test env vars (GATEWAY_URL, SERVER_URL, etc.)
# ==============================================================================

set -u

INTERVAL="${HEALTH_CHECK_INTERVAL:-300}"
CONSECUTIVE_FAILURES=0
ALERT_COOLDOWN=0

log() {
    echo "[$(date -Iseconds)] $1"
}

send_alert() {
    message="$1"
    if [ -z "${ALERT_WEBHOOK_URL:-}" ]; then
        return
    fi

    # Respect cooldown: don't spam alerts more than once every 15 minutes
    now=$(date +%s)
    if [ "$ALERT_COOLDOWN" -gt "$now" ]; then
        log "Alert suppressed (cooldown active until $(date -Iseconds -d @"$ALERT_COOLDOWN" 2>/dev/null || echo "$ALERT_COOLDOWN"))"
        return
    fi

    # Send alert (compatible with Discord & Slack webhooks)
    if curl -sf -X POST "$ALERT_WEBHOOK_URL" \
        -H "Content-Type: application/json" \
        -d "{\"content\":\"$message\"}" \
        2>/dev/null; then
        log "Alert sent successfully"
        ALERT_COOLDOWN=$((now + 900))  # 15 min cooldown
    else
        log "Failed to send alert"
    fi
}

send_recovery_alert() {
    if [ "$CONSECUTIVE_FAILURES" -gt 0 ] && [ -n "${ALERT_WEBHOOK_URL:-}" ]; then
        send_alert "✅ **E-Commerce Health Check RECOVERED** at $(date -Iseconds) — system back to normal after $CONSECUTIVE_FAILURES failed check(s)."
    fi
}

# --- Main Loop ---

log "Health monitor started (interval: ${INTERVAL}s)"
log "Alert webhook: ${ALERT_WEBHOOK_URL:-(not configured)}"

# Initial stabilization delay — services just started, give them a moment
log "Waiting 60s for initial stabilization..."
sleep 60

while true; do
    OUTPUT_FILE=$(mktemp /tmp/smoke-XXXXXX)

    if /bin/sh /smoke-test.sh > "$OUTPUT_FILE" 2>&1; then
        send_recovery_alert
        CONSECUTIVE_FAILURES=0
        log "✅ Health check passed"
    else
        CONSECUTIVE_FAILURES=$((CONSECUTIVE_FAILURES + 1))
        log "❌ Health check FAILED (consecutive failures: $CONSECUTIVE_FAILURES)"
        cat "$OUTPUT_FILE"

        # Extract failed test lines for the alert
        FAILED_TESTS=$(grep "❌" "$OUTPUT_FILE" 2>/dev/null | head -5 | tr '\n' ' ' | tr '"' "'" | tr '\\' ' ')
        send_alert "🔴 **E-Commerce Health Check FAILED** at $(date -Iseconds) (failure #$CONSECUTIVE_FAILURES)\\n\\nFailed: ${FAILED_TESTS:-unknown}"
    fi

    rm -f "$OUTPUT_FILE"
    sleep "$INTERVAL"
done
