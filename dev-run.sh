#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────
# dev-run.sh — Single entry point for the e-commerce platform
#
# Monolith mode uses docker-compose.yml
# Microservice mode uses docker-compose.micro.yml
# Only one mode can run at a time (they share ports).
# ─────────────────────────────────────────────────────────────────────
set -euo pipefail

COMPOSE_FILE="docker-compose.yml"
MICRO_COMPOSE_FILE="docker-compose.micro.yml"
PROJECT="ecom"
PROJECT_MICRO="${PROJECT}-micro"

# Services that backends depend on
INFRA="postgres postgres-chat redis rabbitmq"

# ── Colours ──────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()  { printf "${CYAN}▸${NC} %s\n" "$*"; }
ok()    { printf "${GREEN}✔${NC} %s\n" "$*"; }
warn()  { printf "${YELLOW}⚠${NC} %s\n" "$*"; }
err()   { printf "${RED}✖${NC} %s\n" "$*" >&2; }

# ── Compose wrappers ────────────────────────────────────────────────
dc()       { docker compose -f "$COMPOSE_FILE" -p "$PROJECT" "$@"; }
dc_micro() { docker compose -f "$MICRO_COMPOSE_FILE" -p "$PROJECT_MICRO" "$@"; }

has_flag() { for a in "$@"; do [[ "$a" == "$1" ]] && return 0; done; return 1; }

build_flag() {
    for a in "$@"; do [[ "$a" == "--build" ]] && echo "--build" && return; done
}

# Stop the other mode to avoid port conflicts
stop_mono()  { dc down --remove-orphans 2>/dev/null || true; }
stop_micro() { dc_micro down --remove-orphans 2>/dev/null || true; }

# ── Commands ─────────────────────────────────────────────────────────

cmd_up() {
    stop_micro
    info "Starting full monolith stack..."
    dc up -d $(build_flag "$@")
    ok "Monolith stack is up"
    dc ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
}

cmd_down() {
    info "Stopping all stacks..."
    stop_mono
    stop_micro
    ok "All containers stopped"
}

cmd_infra() {
    info "Starting infrastructure (DB, Redis, RabbitMQ)..."
    dc up -d $INFRA
    ok "Infrastructure is up — run your app from the IDE"
    dc ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
}

cmd_server() {
    stop_micro
    info "Starting server (monolith) + infrastructure..."
    dc up -d $(build_flag "$@") $INFRA server gateway
    ok "Server is up (monolith mode)"
    dc ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
}

cmd_client() {
    info "Starting client + nginx proxy..."
    dc up -d $(build_flag "$@") client nginx-proxy
    ok "Client is up"
    dc ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" | grep -E "NAME|client|nginx" || true
}

cmd_gateway() {
    info "Starting gateway + infrastructure..."
    dc up -d $(build_flag "$@") $INFRA gateway
    ok "Gateway + infra are up"
    dc ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" | grep -E "NAME|gateway|redis" || true
}

cmd_chat() {
    info "Starting chat server + infrastructure..."
    dc up -d $(build_flag "$@") postgres-chat redis rabbitmq chat-server
    ok "Chat server + infra are up"
    dc ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" | grep -E "NAME|chat|redis|rabbit" || true
}

cmd_micro() {
    stop_mono
    info "Starting microservice stack..."
    dc_micro up -d $(build_flag "$@")
    ok "Microservice stack is up"
    dc_micro ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
}

cmd_logs() {
    local service="${1:-}"
    if [[ -n "$service" ]]; then
        dc logs -f "$service" 2>/dev/null || dc_micro logs -f "$service" 2>/dev/null || {
            err "Service '$service' not found in either stack"; exit 1
        }
    else
        dc logs -f 2>/dev/null || dc_micro logs -f 2>/dev/null
    fi
}

cmd_build() {
    local service="${1:-}"
    if [[ -n "$service" ]]; then
        info "Building $service..."
        dc build "$service" 2>/dev/null || dc_micro build "$service" 2>/dev/null || {
            err "Service '$service' not found"; exit 1
        }
    else
        info "Building all services..."
        dc build
    fi
    ok "Build complete"
}

cmd_ps() {
    info "Monolith stack:"
    dc ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "  (not running)"
    echo ""
    info "Microservice stack:"
    dc_micro ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "  (not running)"
}

cmd_restart() {
    local service="${1:-}"
    [[ -z "$service" ]] && { err "Usage: ./dev-run.sh restart <service>"; exit 1; }
    info "Restarting $service..."
    dc restart "$service" 2>/dev/null || dc_micro restart "$service" 2>/dev/null || {
        err "Service '$service' not found"; exit 1
    }
    ok "$service restarted"
}

# ── Usage ────────────────────────────────────────────────────────────

usage() {
    cat <<'EOF'

  Usage: ./dev-run.sh <command> [--build]

  Start / stop
    up    [--build]    Full monolith stack (all services)
    down               Stop everything (both modes)
    server [--build]   Server + gateway + infra (monolith mode)
    client [--build]   Client + nginx proxy only
    gateway [--build]  Gateway + infra only
    chat   [--build]   Chat server + infra only
    micro  [--build]   Full microservice stack
    infra              Infrastructure only (DB, Redis, RabbitMQ)

  Utilities
    logs  [service]    Tail logs (all or one service)
    build [service]    Build images without starting
    ps                 Show running containers (both modes)
    restart <service>  Restart a specific service

  --build forces an image rebuild before starting.

  Examples:
    ./dev-run.sh up                 # everything, monolith mode
    ./dev-run.sh server --build     # rebuild + start backend
    ./dev-run.sh micro              # switch to microservice mode
    ./dev-run.sh infra              # just DBs (run app from IDE)
    ./dev-run.sh logs server        # tail server logs

EOF
    exit 0
}

# ── Main ─────────────────────────────────────────────────────────────

[[ $# -lt 1 ]] && usage

case "$1" in
    up)      shift; cmd_up "$@" ;;
    down)    cmd_down ;;
    server)  shift; cmd_server "$@" ;;
    client)  shift; cmd_client "$@" ;;
    gateway) shift; cmd_gateway "$@" ;;
    chat)    shift; cmd_chat "$@" ;;
    micro)   shift; cmd_micro "$@" ;;
    infra)   cmd_infra ;;
    logs)    shift; cmd_logs "$@" ;;
    build)   shift; cmd_build "$@" ;;
    ps)      cmd_ps ;;
    restart) shift; cmd_restart "$@" ;;
    help|-h|--help) usage ;;
    *)       err "Unknown command: $1"; usage ;;
esac
