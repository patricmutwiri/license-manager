#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${LICENSE_MANAGER_URL:-http://localhost:8080}"
ADMIN_KEY="${LICENSE_ADMIN_API_KEY:-}"
CLIENT_KEY="${LICENSE_CLIENT_KEY:-}"

usage() {
  cat <<'USAGE'
License Manager CLI

Environment:
  LICENSE_MANAGER_URL       Base URL, defaults to http://localhost:8080
  LICENSE_ADMIN_API_KEY     Admin API key for admin commands
  LICENSE_CLIENT_KEY        Runtime client token for runtime commands

Commands:
  health
  admin:list-licenses
  admin:list-products
  admin:audit-events
  runtime:validate <license_key> <product_code> <fingerprint>
  runtime:heartbeat <license_key> <fingerprint>

USAGE
}

require_admin() {
  if [[ -z "$ADMIN_KEY" ]]; then
    echo "LICENSE_ADMIN_API_KEY is required" >&2
    exit 2
  fi
}

require_client() {
  if [[ -z "$CLIENT_KEY" ]]; then
    echo "LICENSE_CLIENT_KEY is required" >&2
    exit 2
  fi
}

json_post() {
  local path="$1"
  local body="$2"
  curl -sS -X POST "$BASE_URL$path" \
    -H 'Content-Type: application/json' \
    -H "X-License-Client-Key: $CLIENT_KEY" \
    -d "$body"
}

case "${1:-}" in
  health)
    curl -sS "$BASE_URL/actuator/health"
    ;;
  admin:list-licenses)
    require_admin
    curl -sS "$BASE_URL/api/v1/admin/licenses" -H "X-Admin-Api-Key: $ADMIN_KEY"
    ;;
  admin:list-products)
    require_admin
    curl -sS "$BASE_URL/api/v1/admin/products" -H "X-Admin-Api-Key: $ADMIN_KEY"
    ;;
  admin:audit-events)
    require_admin
    curl -sS "$BASE_URL/api/v1/admin/audit-events" -H "X-Admin-Api-Key: $ADMIN_KEY"
    ;;
  runtime:validate)
    require_client
    [[ $# -eq 4 ]] || { usage; exit 2; }
    json_post "/api/v1/runtime/licenses/validate" \
      "{\"key\":\"$2\",\"productCode\":\"$3\",\"fingerprint\":\"$4\"}"
    ;;
  runtime:heartbeat)
    require_client
    [[ $# -eq 3 ]] || { usage; exit 2; }
    json_post "/api/v1/runtime/licenses/$2/machines/heartbeat" \
      "{\"fingerprint\":\"$3\"}"
    ;;
  *)
    usage
    exit 2
    ;;
esac
