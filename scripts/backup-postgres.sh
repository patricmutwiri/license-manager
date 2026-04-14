#!/usr/bin/env bash
set -euo pipefail

: "${SPRING_DATASOURCE_URL:?SPRING_DATASOURCE_URL is required}"

backup_dir="${BACKUP_DIR:-backups}"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_file="${backup_dir}/license-manager-${timestamp}.dump"

mkdir -p "${backup_dir}"
pg_dump --format=custom --no-owner --no-privileges --file="${backup_file}" "${SPRING_DATASOURCE_URL}"
printf 'Backup written to %s\n' "${backup_file}"
