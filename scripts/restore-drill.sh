#!/usr/bin/env bash
set -euo pipefail

: "${RESTORE_DRILL_DATABASE_URL:?RESTORE_DRILL_DATABASE_URL is required}"

if [[ $# -ne 1 ]]; then
  printf 'Usage: %s <backup.dump>\n' "$0" >&2
  exit 64
fi

backup_file="$1"

if [[ ! -f "${backup_file}" ]]; then
  printf 'Backup file not found: %s\n' "${backup_file}" >&2
  exit 66
fi

SPRING_DATASOURCE_URL="${RESTORE_DRILL_DATABASE_URL}" \
  "$(dirname "$0")/restore-postgres.sh" "${backup_file}"

SPRING_DATASOURCE_URL="${RESTORE_DRILL_DATABASE_URL}" \
SPRING_JPA_HIBERNATE_DDL_AUTO=validate \
SPRING_FLYWAY_ENABLED=true \
LICENSE_SEED_ENABLED=false \
mvn -q -Dtest=MigrationValidationTests test

printf 'Restore drill completed for %s\n' "${backup_file}"
