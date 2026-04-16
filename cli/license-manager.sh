#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${LICENSE_MANAGER_URL:-http://localhost:8080}"
ADMIN_KEY="${LICENSE_ADMIN_API_KEY:-}"
ACTOR_ID="${LICENSE_ACTOR_USER_ID:-}"
CLIENT_KEY="${LICENSE_CLIENT_KEY:-}"

usage() {
  cat <<'USAGE'
License Manager CLI

Environment:
  LICENSE_MANAGER_URL       Base URL, defaults to http://localhost:8080
  LICENSE_ADMIN_API_KEY     Admin API key for admin commands
  LICENSE_ACTOR_USER_ID     Optional actor user ID for RBAC-scoped admin commands
  LICENSE_CLIENT_KEY        Runtime client token for runtime commands

Commands:
  health
  admin:list-users
  admin:list-organizations
  admin:list-products
  admin:list-policies
  admin:list-licenses
  admin:list-audit-events
  admin:list-billing-plans
  admin:create-product <organization_id> <code> <name>
  admin:create-entitlement <product_id> <code> <name>
  admin:create-policy <product_id> <code> <name> <NODE_LOCKED|FLOATING> <max_machines> <max_seats>
  admin:issue-license <user_id> <organization_id> <policy_id> <customer_name> <customer_email>
  admin:set-license-status <license_id> <ACTIVE|INACTIVE|SUSPENDED|EXPIRED|REVOKED>
  admin:create-client-token <name> <product_id> <comma_scopes>
  admin:rotate-client-token <token_id> <comma_scopes>
  admin:revoke-client-token <token_id>
  admin:create-billing-plan <code> <name> <policy_id> <amount_cents> <currency> <MONTHLY|YEARLY|ONE_TIME>
  runtime:validate <license_key> <product_code> <fingerprint> [version]
  runtime:activate <license_key> <fingerprint> [name] [platform] [version]
  runtime:heartbeat <license_key> <fingerprint> [version]
  runtime:offline-checkout <license_key> <fingerprint> [ttl_days]

USAGE
}

require_admin() {
  if [[ -z "${ADMIN_KEY}" ]]; then
    echo "LICENSE_ADMIN_API_KEY is required" >&2
    exit 2
  fi
}

require_client() {
  if [[ -z "${CLIENT_KEY}" ]]; then
    echo "LICENSE_CLIENT_KEY is required" >&2
    exit 2
  fi
}

json_escape() {
  printf '%s' "${1:-}" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

json_string_array() {
  local csv="${1:-}"
  local output="["
  local separator=""
  IFS=',' read -r -a items <<< "${csv}"
  for raw in "${items[@]}"; do
    local item
    item="$(echo "${raw}" | xargs)"
    [[ -z "${item}" ]] && continue
    output+="${separator}\"$(json_escape "${item}")\""
    separator=","
  done
  output+="]"
  printf '%s' "${output}"
}

admin_headers=()
runtime_headers=()

build_headers() {
  admin_headers=(-H "X-Admin-Api-Key: ${ADMIN_KEY}")
  if [[ -n "${ACTOR_ID}" ]]; then
    admin_headers+=(-H "X-Actor-User-Id: ${ACTOR_ID}")
  fi
  runtime_headers=(-H "Authorization: Bearer ${CLIENT_KEY}")
}

get_admin() {
  require_admin
  build_headers
  curl -sS --fail "${BASE_URL}${1}" "${admin_headers[@]}"
}

send_admin() {
  require_admin
  build_headers
  local method="$1"
  local path="$2"
  local body="${3:-{}}"
  curl -sS --fail -X "${method}" "${BASE_URL}${path}" \
    -H 'Content-Type: application/json' \
    "${admin_headers[@]}" \
    -d "${body}"
}

post_runtime() {
  require_client
  build_headers
  local path="$1"
  local body="$2"
  curl -sS --fail -X POST "${BASE_URL}${path}" \
    -H 'Content-Type: application/json' \
    "${runtime_headers[@]}" \
    -d "${body}"
}

case "${1:-}" in
  health)
    curl -sS --fail "${BASE_URL}/actuator/health"
    ;;
  admin:list-users)
    get_admin "/api/v1/admin/users"
    ;;
  admin:list-organizations)
    get_admin "/api/v1/admin/organizations"
    ;;
  admin:list-products)
    get_admin "/api/v1/admin/products"
    ;;
  admin:list-policies)
    get_admin "/api/v1/admin/policies"
    ;;
  admin:list-licenses)
    get_admin "/api/v1/admin/licenses"
    ;;
  admin:list-audit-events | admin:audit-events)
    get_admin "/api/v1/admin/audit-events"
    ;;
  admin:list-billing-plans)
    get_admin "/api/v1/admin/billing/plans"
    ;;
  admin:create-product)
    [[ $# -eq 4 ]] || { usage; exit 2; }
    send_admin POST "/api/v1/admin/products" \
      "{\"organizationId\":$2,\"code\":\"$(json_escape "$3")\",\"name\":\"$(json_escape "$4")\",\"description\":\"$(json_escape "$4")\"}"
    ;;
  admin:create-entitlement)
    [[ $# -eq 4 ]] || { usage; exit 2; }
    send_admin POST "/api/v1/admin/products/$2/entitlements" \
      "{\"code\":\"$(json_escape "$3")\",\"name\":\"$(json_escape "$4")\"}"
    ;;
  admin:create-policy)
    [[ $# -eq 8 ]] || { usage; exit 2; }
    send_admin POST "/api/v1/admin/policies" \
      "{\"productId\":$2,\"code\":\"$(json_escape "$3")\",\"name\":\"$(json_escape "$4")\",\"licensingModel\":\"$(json_escape "$5")\",\"maxMachines\":$6,\"maxSeats\":$7,\"validityDays\":365,\"heartbeatIntervalMinutes\":60,\"heartbeatGracePeriodMinutes\":180,\"offlineTtlDays\":7}"
    ;;
  admin:issue-license)
    [[ $# -eq 6 ]] || { usage; exit 2; }
    send_admin POST "/api/v1/admin/licenses" \
      "{\"userId\":$2,\"organizationId\":$3,\"policyId\":$4,\"customerName\":\"$(json_escape "$5")\",\"customerEmail\":\"$(json_escape "$6")\"}"
    ;;
  admin:set-license-status)
    [[ $# -eq 3 ]] || { usage; exit 2; }
    send_admin PATCH "/api/v1/admin/licenses/$2/status" "{\"status\":\"$(json_escape "$3")\"}"
    ;;
  admin:create-client-token)
    [[ $# -eq 4 ]] || { usage; exit 2; }
    send_admin POST "/api/v1/admin/client-tokens" \
      "{\"name\":\"$(json_escape "$2")\",\"productId\":$3,\"scopes\":$(json_string_array "$4")}"
    ;;
  admin:rotate-client-token)
    [[ $# -eq 3 ]] || { usage; exit 2; }
    send_admin POST "/api/v1/admin/client-tokens/$2/rotate" "{\"scopes\":$(json_string_array "$3")}"
    ;;
  admin:revoke-client-token)
    [[ $# -eq 2 ]] || { usage; exit 2; }
    send_admin POST "/api/v1/admin/client-tokens/$2/revoke"
    ;;
  admin:create-billing-plan)
    [[ $# -eq 7 ]] || { usage; exit 2; }
    send_admin POST "/api/v1/admin/billing/plans" \
      "{\"code\":\"$(json_escape "$2")\",\"name\":\"$(json_escape "$3")\",\"policyId\":$4,\"amountCents\":$5,\"currency\":\"$(json_escape "$6")\",\"billingInterval\":\"$(json_escape "$7")\",\"provider\":\"INTERNAL\"}"
    ;;
  runtime:validate)
    [[ $# -ge 4 && $# -le 5 ]] || { usage; exit 2; }
    post_runtime "/api/v1/runtime/licenses/validate" \
      "{\"key\":\"$(json_escape "$2")\",\"productCode\":\"$(json_escape "$3")\",\"fingerprint\":\"$(json_escape "$4")\",\"version\":\"$(json_escape "${5:-}")\"}"
    ;;
  runtime:activate)
    [[ $# -ge 3 && $# -le 6 ]] || { usage; exit 2; }
    post_runtime "/api/v1/runtime/licenses/$2/machines" \
      "{\"fingerprint\":\"$(json_escape "$3")\",\"name\":\"$(json_escape "${4:-}")\",\"platform\":\"$(json_escape "${5:-}")\",\"version\":\"$(json_escape "${6:-}")\"}"
    ;;
  runtime:heartbeat)
    [[ $# -ge 3 && $# -le 4 ]] || { usage; exit 2; }
    post_runtime "/api/v1/runtime/licenses/$2/machines/heartbeat" \
      "{\"fingerprint\":\"$(json_escape "$3")\",\"version\":\"$(json_escape "${4:-}")\"}"
    ;;
  runtime:offline-checkout)
    [[ $# -ge 3 && $# -le 4 ]] || { usage; exit 2; }
    post_runtime "/api/v1/runtime/offline/checkouts" \
      "{\"key\":\"$(json_escape "$2")\",\"fingerprint\":\"$(json_escape "$3")\",\"ttlDays\":${4:-3}}"
    ;;
  *)
    usage
    exit 2
    ;;
esac
