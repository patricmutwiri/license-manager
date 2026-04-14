# Deployment Runbook

## Environments
- Development: H2 tests, optional local PostgreSQL, seed data enabled, email may be disabled.
- Staging: PostgreSQL, stable offline keys, Redis/Upstash, OAuth clients, SMTP sandbox, production profile.
- Production: PostgreSQL HA, Redis/Upstash, stable offline keys, external IdP, SMTP, backups, metrics scraping, alerting.

## Health Probes
- Liveness: `GET /actuator/health/liveness`
- Readiness: `GET /actuator/health/readiness`
- Aggregate health: `GET /actuator/health`

## Metrics
- Prometheus: `GET /actuator/prometheus`
- Alert on sustained 5xx responses, validation denial spikes, auth failures, Redis limiter failures, scheduler errors, DB saturation, and latency p95 degradation.

## Backup
- PostgreSQL logical backup: `scripts/backup-postgres.sh`
- PostgreSQL restore: `scripts/restore-postgres.sh backups/license-manager-<timestamp>.dump`
- Kubernetes scheduled backup: `deploy/kubernetes/postgres-backup-cronjob.yaml`
- Restore drill: restore into an isolated database, run Flyway validation, run `mvn clean test`, and verify critical API flows.
- Store offline signing keys in a secret manager and include key-rotation procedures outside database backups.

## Rollout
- Kubernetes baseline manifests live under `deploy/kubernetes/`.
- Apply migrations before serving traffic.
- Run one instance with scheduler enabled, or ensure scheduler locks are introduced before multi-active scheduler deployment.
- Keep `LICENSE_RATE_LIMIT_FAIL_OPEN=false` in production.
