#!/usr/bin/env bash
set -euo pipefail

: "${SPRING_DATASOURCE_URL:?SPRING_DATASOURCE_URL is required}"

if [[ $# -ne 1 ]]; then
  printf 'Usage: %s <backup.dump>\n' "$0" >&2
  exit 64
fi

backup_file="$1"

if [[ ! -f "${backup_file}" ]]; then
  printf 'Backup file not found: %s\n' "${backup_file}" >&2
  exit 66
fi

pg_restore --clean --if-exists --no-owner --no-privileges --dbname="${SPRING_DATASOURCE_URL}" "${backup_file}"
printf 'Restore completed from %s\n' "${backup_file}"
