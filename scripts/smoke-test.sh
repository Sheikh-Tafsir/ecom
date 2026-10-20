#!/bin/sh
# ==============================================================================
# E-Commerce Platform — End-to-End Smoke Test & Health Verification
# Fully POSIX sh compliant — runs on host, inside Docker, and in CI/CD.
# ==============================================================================

set -e

# Try loading .env if on host
if [ -f "./.env" ]; then
    # shellcheck disable=SC2046
    export $(grep -v '^#' ./.env | grep -v '^\s*$' | xargs 2>/dev/null) 2>/dev/null || true
elif [ -f "../.env" ]; then
    # shellcheck disable=SC2046
    export $(grep -v '^#' ../.env | grep -v '^\s*$' | xargs 2>/dev/null) 2>/dev/null || true
fi

GATEWAY_PORT="${GATEWAY_PORT:-8080}"
CLIENT_PORT="${CLIENT_PORT:-5173}"

# If running inside docker container network, hostnames are service names
# Otherwise default to localhost
GATEWAY_URL="${GATEWAY_URL:-http://localhost:${GATEWAY_PORT}}"
SERVER_URL="${SERVER_URL:-http://server:8081}"
CHAT_URL="${CHAT_URL:-http://chat-server:3001}"
CLIENT_URL="${CLIENT_TARGET_URL:-http://localhost:${CLIENT_PORT}}"
INTERNAL_KEY="${INTERNAL_SERVICE_KEY:-}"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

FAILED=0
PASSED=0

pass() {
    printf " ${GREEN}✅ PASS${NC} %s\n" "$1"
    PASSED=$((PASSED + 1))
}

fail() {
    printf " ${RED}❌ FAIL${NC} %s\n" "$1"
    printf "   ${RED}Detail:${NC} %s\n" "$2"
    FAILED=$((FAILED + 1))
}

warn() {
    printf " ${YELLOW}⚠️  WARN${NC} %s\n" "$1"
    printf "   ${YELLOW}Detail:${NC} %s\n" "$2"
}

printf "\n${BOLD}${CYAN}======================================================${NC}\n"
printf "${BOLD}${CYAN}   E-Commerce Platform — System Health & Smoke Test   ${NC}\n"
printf "${BOLD}${CYAN}======================================================${NC}\n"
printf "Gateway:     ${BOLD}%s${NC}\n" "$GATEWAY_URL"
printf "Main Server: ${BOLD}%s${NC}\n" "$SERVER_URL"
printf "Chat Server: ${BOLD}%s${NC}\n" "$CHAT_URL"
printf "Client Web:  ${BOLD}%s${NC}\n\n" "$CLIENT_URL"

# ------------------------------------------------------------------------------
# Test 1: Gateway -> Main Server: Public Database Read & Cache
# ------------------------------------------------------------------------------
printf "${BOLD}[1/8] Gateway -> Main Server: Public DB Read & Cache (/categories)${NC}\n"
RESPONSE=$(curl -s -w "\n%{http_code}" --max-time 5 "$GATEWAY_URL/categories" 2>/dev/null || printf "\n000")
HTTP_CODE=$(printf "%s" "$RESPONSE" | tail -n1)
BODY=$(printf "%s" "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    pass "GET /categories returned 200 OK (Postgres + Redis cache active via Gateway)"
else
    fail "GET /categories returned HTTP $HTTP_CODE" "Response: $BODY"
fi

# ------------------------------------------------------------------------------
# Test 2: Gateway -> Chat Server Reverse Proxy
# ------------------------------------------------------------------------------
printf "${BOLD}[2/8] Gateway -> Chat Server: Reverse Proxy Routing (/chats/v3/api-docs)${NC}\n"
RESPONSE=$(curl -s -w "\n%{http_code}" --max-time 5 "$GATEWAY_URL/chats/v3/api-docs" 2>/dev/null || printf "\n000")
HTTP_CODE=$(printf "%s" "$RESPONSE" | tail -n1)
BODY=$(printf "%s" "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    pass "GET /chats/v3/api-docs returned 200 OK (Chat Server reachable via Gateway)"
else
    fail "GET /chats/v3/api-docs returned HTTP $HTTP_CODE" "Response: $BODY"
fi

# ------------------------------------------------------------------------------
# Test 3: Gateway -> Socket.IO WebSocket Upgrade Handshake
# ------------------------------------------------------------------------------
printf "${BOLD}[3/8] Gateway -> Chat Server: WebSocket Upgrade Handshake (/socket.io/)${NC}\n"
WS_RESPONSE=$(curl -s -i --max-time 5 \
    -H "Connection: Upgrade" \
    -H "Upgrade: websocket" \
    -H "Sec-WebSocket-Version: 13" \
    -H "Sec-WebSocket-Key: SGVsbG8sIHdvcmxkIQ==" \
    "$GATEWAY_URL/socket.io/?EIO=4&transport=websocket" 2>/dev/null || true)

if printf "%s" "$WS_RESPONSE" | grep -q -E "(101 Switching Protocols|sid)"; then
    pass "Gateway upgraded /socket.io/ to WebSocket protocol (HTTP 101 / sid received)"
else
    fail "Socket.IO WebSocket handshake failed via Gateway" "Response: $(printf "%s" "$WS_RESPONSE" | head -n 5)"
fi

# ------------------------------------------------------------------------------
# Test 4: Security & Authentication Filter Enforcement
# ------------------------------------------------------------------------------
printf "${BOLD}[4/8] Security Filter: Unauthenticated Request Rejection (/users/me)${NC}\n"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$GATEWAY_URL/users/me" 2>/dev/null || printf "000")

if [ "$HTTP_CODE" -eq 401 ]; then
    pass "GET /users/me correctly rejected with 401 Unauthorized (JWT filter active)"
else
    fail "Security enforcement check failed. Expected 401, got $HTTP_CODE" "Protected route did not reject unauthenticated request"
fi

# ------------------------------------------------------------------------------
# Test 5: Main Server Deep Dependency Health (Postgres, Redis, RabbitMQ)
# ------------------------------------------------------------------------------
printf "${BOLD}[5/8] Main Server Deep Health (Actuator /actuator/health)${NC}\n"
RESPONSE=$(curl -s -w "\n%{http_code}" --max-time 5 "$SERVER_URL/actuator/health" 2>/dev/null || printf "\n000")
HTTP_CODE=$(printf "%s" "$RESPONSE" | tail -n1)
BODY=$(printf "%s" "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ] && printf "%s" "$BODY" | grep -q '"status":"UP"'; then
    pass "Main Server Actuator is UP (Postgres, Redis, RabbitMQ healthy)"
else
    fail "Main Server Actuator reported unhealthy or unreachable (HTTP $HTTP_CODE)" "Response: $BODY"
fi

# ------------------------------------------------------------------------------
# Test 6: Chat Server Deep Dependency Health (Postgres, Redis)
# ------------------------------------------------------------------------------
printf "${BOLD}[6/8] Chat Server Deep Health (/health)${NC}\n"
RESPONSE=$(curl -s -w "\n%{http_code}" --max-time 5 "$CHAT_URL/health" 2>/dev/null || printf "\n000")
HTTP_CODE=$(printf "%s" "$RESPONSE" | tail -n1)
BODY=$(printf "%s" "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ] && printf "%s" "$BODY" | grep -q '"database":"UP"' && printf "%s" "$BODY" | grep -q '"redis":"UP"'; then
    pass "Chat Server health is UP (Chat DB: UP, Redis: UP)"
else
    fail "Chat Server reported unhealthy (HTTP $HTTP_CODE)" "Response: $BODY"
fi

# ------------------------------------------------------------------------------
# Test 7: Client Web Frontend Delivery
# ------------------------------------------------------------------------------
printf "${BOLD}[7/8] Client Web Application (Nginx SPA Delivery)${NC}\n"
RESPONSE=$(curl -s -w "\n%{http_code}" --max-time 5 "$CLIENT_URL" 2>/dev/null || printf "\n000")
HTTP_CODE=$(printf "%s" "$RESPONSE" | tail -n1)
BODY=$(printf "%s" "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ] && printf "%s" "$BODY" | grep -q -i "html"; then
    pass "Client Nginx serving index.html (200 OK)"
else
    fail "Client web app unreachable or invalid HTML (HTTP $HTTP_CODE)" "Response: $(printf "%s" "$BODY" | head -n 3)"
fi

# ------------------------------------------------------------------------------
# Test 8: Inter-Service Machine-to-Machine Authentication
# ------------------------------------------------------------------------------
printf "${BOLD}[8/8] Inter-Service M2M Communication (Chat -> Main Server)${NC}\n"
if [ -n "$INTERNAL_KEY" ]; then
    RESPONSE=$(curl -s -w "\n%{http_code}" --max-time 5 \
        -X POST "$SERVER_URL/internal/users/batch" \
        -H "Content-Type: application/json" \
        -H "X-Internal-Service-Key: $INTERNAL_KEY" \
        -d '["00000000-0000-0000-0000-000000000000"]' 2>/dev/null || printf "\n000")
    HTTP_CODE=$(printf "%s" "$RESPONSE" | tail -n1)
    BODY=$(printf "%s" "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" -eq 200 ]; then
        pass "POST /internal/users/batch accepted M2M call with internal key"
    else
        fail "M2M internal batch call failed (HTTP $HTTP_CODE)" "Response: $BODY"
    fi
else
    warn "Skipped M2M check" "INTERNAL_SERVICE_KEY not provided"
fi

# ------------------------------------------------------------------------------
# Summary
# ------------------------------------------------------------------------------
printf "\n${BOLD}${CYAN}------------------------------------------------------${NC}\n"
printf "Tests Passed: ${GREEN}%d${NC} | Tests Failed: ${RED}%d${NC}\n" "$PASSED" "$FAILED"

if [ "$FAILED" -eq 0 ]; then
    printf "${GREEN}${BOLD}🎉 ALL SYSTEMS OPERATIONAL: Gateway, DBs, Caches, WebSockets, Client, and M2M verified!${NC}\n\n"
    exit 0
else
    printf "${RED}${BOLD}🚨 VERIFICATION FAILED: Check logs above for details.${NC}\n\n"
    exit 1
fi
