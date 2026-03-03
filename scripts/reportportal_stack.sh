#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="${MS_TEST_RP_RUNTIME_DIR:-$ROOT_DIR/.runtime/reportportal}"
COMPOSE_FILE="$RUNTIME_DIR/docker-compose.yml"
PROJECT_NAME="${MS_TEST_RP_PROJECT:-ms-test-rp}"
PORT="${MS_TEST_RP_PORT:-28080}"
TRAEFIK_PORT="${MS_TEST_RP_TRAEFIK_PORT:-28081}"
HTTPS_PORT="${MS_TEST_RP_HTTPS_PORT:-28443}"
BASE_URL="${MS_TEST_RP_BASE_URL:-http://127.0.0.1:${PORT}}"
ADMIN_USER="${MS_TEST_RP_ADMIN_USER:-default}"
ADMIN_PASSWORD="${MS_TEST_RP_ADMIN_PASSWORD:-1q2w3e}"
CLIENT_ID="${MS_TEST_RP_CLIENT_ID:-ui}"
CLIENT_SECRET="${MS_TEST_RP_CLIENT_SECRET:-uiman}"

compose() {
  docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" --profile core "$@"
}

ensure_prerequisites() {
  command -v curl >/dev/null || { echo "curl is required" >&2; exit 1; }
  command -v jq >/dev/null || { echo "jq is required" >&2; exit 1; }
}

ensure_compose_file() {
  mkdir -p "$RUNTIME_DIR"
  if [[ ! -f "$COMPOSE_FILE" ]]; then
    curl -fsSL https://raw.githubusercontent.com/reportportal/reportportal/master/docker-compose.yml \
      -o "$COMPOSE_FILE"
  fi
  sed -i \
    -e 's/"8080:8080"/"${MS_TEST_RP_PORT:-28080}:8080"/g' \
    -e 's/"8081:8081"/"${MS_TEST_RP_TRAEFIK_PORT:-28081}:8081"/g' \
    -e 's/"443:443"/"${MS_TEST_RP_HTTPS_PORT:-28443}:443"/g' \
    -E -e 's/\$\{MS_TEST_RP_PORT:-[0-9]+\}/\${MS_TEST_RP_PORT:-28080}/g' \
    -E -e 's/\$\{MS_TEST_RP_TRAEFIK_PORT:-[0-9]+\}/\${MS_TEST_RP_TRAEFIK_PORT:-28081}/g' \
    -E -e 's/\$\{MS_TEST_RP_HTTPS_PORT:-[0-9]+\}/\${MS_TEST_RP_HTTPS_PORT:-28443}/g' \
    -e '/container_name:\s*\*db_host/d' \
    "$COMPOSE_FILE"
}

fetch_token() {
  local raw
  raw="$(curl -sS -X POST "$BASE_URL/uat/sso/oauth/token" \
    -u "$CLIENT_ID:$CLIENT_SECRET" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data "grant_type=password&username=$ADMIN_USER&password=$ADMIN_PASSWORD" 2>/dev/null || true)"
  if [[ -z "$raw" ]]; then
    return 1
  fi
  local token
  token="$(echo "$raw" | jq -r '.access_token // empty' 2>/dev/null || true)"
  [[ -n "$token" ]] || return 1
  echo "$token"
}

wait_until_ready() {
  local token
  for _ in {1..120}; do
    token="$(fetch_token 2>/dev/null || true)"
    if [[ -n "$token" && "$token" != "null" ]]; then
      if curl -fsS "$BASE_URL/api/v1/default_personal/launch" \
        -H "Authorization: bearer $token" >/dev/null 2>&1; then
        return 0
      fi
    fi
    sleep 2
  done
  echo "ReportPortal did not become ready in time" >&2
  return 1
}

cmd_start() {
  ensure_prerequisites
  ensure_compose_file
  compose up -d
  wait_until_ready
  echo "ReportPortal ready on $BASE_URL"
}

cmd_stop() {
  ensure_compose_file
  compose down
}

cmd_status() {
  ensure_compose_file
  compose ps
}

cmd_token() {
  ensure_prerequisites
  ensure_compose_file
  for _ in {1..30}; do
    if fetch_token; then
      return 0
    fi
    sleep 2
  done
  echo "Unable to fetch ReportPortal token" >&2
  exit 1
}

usage() {
  cat <<'TXT'
Usage: scripts/reportportal_stack.sh <start|stop|status|token>

Environment overrides:
  MS_TEST_RP_PROJECT           Compose project name (default: ms-test-rp)
  MS_TEST_RP_RUNTIME_DIR       Compose runtime dir (default: .runtime/reportportal)
  MS_TEST_RP_BASE_URL          ReportPortal URL (default: http://127.0.0.1:8080)
  MS_TEST_RP_PORT              Gateway host port (default: 28080)
  MS_TEST_RP_TRAEFIK_PORT      Traefik dashboard port (default: 28081)
  MS_TEST_RP_HTTPS_PORT        HTTPS port (default: 28443)
  MS_TEST_RP_ADMIN_USER        ReportPortal user (default: default)
  MS_TEST_RP_ADMIN_PASSWORD    ReportPortal password (default: 1q2w3e)
  MS_TEST_RP_CLIENT_ID         OAuth client id (default: ui)
  MS_TEST_RP_CLIENT_SECRET     OAuth client secret (default: uiman)
TXT
}

ACTION="${1:-}"
case "$ACTION" in
  start) cmd_start ;;
  stop) cmd_stop ;;
  status) cmd_status ;;
  token) cmd_token ;;
  *) usage; exit 1 ;;
esac
