# License Manager

Production-oriented licensing platform built with Java 21, Spring Boot 4, Maven, Spring MVC, Spring Security OAuth2, Thymeleaf, and Spring Data JPA.

The platform now supports Keygen-like concepts: products, policies, licenses, machines, feature entitlements, lifecycle states, activation limits, machine heartbeats, signed offline artifacts, audit events, and admin/runtime REST APIs.

## Features

- Product and policy-driven license issuance.
- License lifecycle: `ACTIVE`, `INACTIVE`, `SUSPENDED`, `EXPIRED`, `REVOKED`.
- Node-locked and floating/seat-based activation models.
- Fingerprint-scoped machine activation, deactivation, and validation.
- Heartbeat/check-in tracking with missed-heartbeat seat reclamation.
- Ed25519 signed offline license artifacts with TTL and local public-key verification.
- Feature entitlement attachment at policy level.
- Audit trail for products, policies, licenses, machines, heartbeat misses, and offline checkouts.
- OAuth2-backed Thymeleaf dashboard plus an operations console for accounts, products, lifecycle, machines, runtime access, billing, and audit activity.
- API-first admin/runtime flows with JSON responses.
- Flyway-managed schema, scoped runtime client tokens, Redis-capable rate limiting, scheduled operations jobs, Prometheus metrics, and service coverage enforcement.
- Billing-ready plan/subscription domain with provider integration points.
- OpenAPI governance artifact, shell CLI, and Java runtime SDK starter.

## Requirements

- Java 21+
- Maven 3.8+
- PostgreSQL for normal runtime
- SMTP account if email backups are enabled
- Google/GitHub OAuth2 credentials if using the browser login flow

## Environment

Set these for local or production runtime. Do not commit real values.

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/license_manager'
export SPRING_DATASOURCE_USERNAME='postgres'
export SPRING_DATASOURCE_PASSWORD='change-me'
export SPRING_JPA_HIBERNATE_DDL_AUTO='validate'
export SPRING_FLYWAY_ENABLED='true'

export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID='...'
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET='...'
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_ID='...'
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_SECRET='...'

export SMTP_HOST='mail.example.com'
export SMTP_PORT='465'
export SMTP_USER='api@example.com'
export SMTP_PASS='change-me'
export SMTP_AUTH='true'
export SMTP_TLS='true'
export SMTP_MAIL_FROM='noreply@example.com'

export LICENSE_ADMIN_API_KEY='replace-with-long-random-secret'
export LICENSE_OFFLINE_PRIVATE_KEY_BASE64='<pkcs8-ed25519-private-key>'
export LICENSE_OFFLINE_PUBLIC_KEY_BASE64='<x509-ed25519-public-key>'
export LICENSE_SEED_ENABLED='true'
export LICENSE_EMAIL_ENABLED='true'
export LICENSE_RATE_LIMIT_REDIS_URL='<redis-or-rediss-url>'
export LICENSE_RATE_LIMIT_REDIS_KEY_PREFIX='license-manager:rate-limit'
export LICENSE_RATE_LIMIT_RUNTIME_PER_MINUTE='120'
export LICENSE_RATE_LIMIT_FAIL_OPEN='false'
export LICENSE_JOBS_ENABLED='true'
export LICENSE_JOBS_EXPIRY_SWEEP_DELAY_MS='300000'
export LICENSE_JOBS_HEARTBEAT_CLEANUP_DELAY_MS='300000'
export LICENSE_JOBS_STALE_MACHINE_CLEANUP_DELAY_MS='3600000'
export LICENSE_JOBS_SUBSCRIPTION_EXPIRY_DELAY_MS='3600000'
export PORT='8080'
```

The app also accepts the older `SMTP_MAIL_HOST`, `SMTP_MAIL_PORT`, `SMTP_MAIL_USERNAME`, `SMTP_MAIL_PASSWORD`, `SMTP_MAIL_AUTH`, and `SMTP_MAIL_TLS` names. Production profile defaults live in `src/main/resources/application-prod.yml` and intentionally use unresolved placeholders for required secrets so production startup fails fast when configuration is incomplete.

If offline key env vars are omitted in local development, the app generates an ephemeral Ed25519 key pair at startup. Production should use stable keys so issued artifacts remain verifiable across restarts.

## Run Locally

```bash
createdb license_manager
mvn clean test
mvn spring-boot:run
```

Open the dashboard at `http://localhost:8080`.

Seed data is created when `LICENSE_SEED_ENABLED=true`: a demo user, organization, product, entitlements, floating policy, and license.

The operations console is available at `http://localhost:8080/admin` for authenticated users whose stored role is `ADMIN`. It shows dashboard health signals, account/customer views, products and policies, license lifecycle state, machine heartbeat state, runtime tokens, billing plans/subscriptions, and audit activity. Write workflows remain API-first for explicit RBAC and automation.

## Admin API

All admin endpoints require:

```http
X-Admin-Api-Key: <LICENSE_ADMIN_API_KEY>
```

For actor-scoped RBAC, also send:

```http
X-Actor-User-Id: <user id>
```

When `X-Actor-User-Id` is present, global `ADMIN` users can administer the platform, and organization members are evaluated through this matrix:

| Organization Role | Permissions |
| --- | --- |
| `OWNER` | All organization permissions |
| `ADMIN` | Organization, membership, product, policy, license, machine, audit, billing, and client-token management |
| `BILLING` | Organization read, license read, audit read, billing management |
| `DEVELOPER` | Organization read, license read, machine read, client-token management |
| `SUPPORT` | Organization read, license read/update, machine read, audit read |
| `VIEWER` | Organization read, license read, machine read, audit read |

Create a customer, organization, and product:

```bash
curl -X POST http://localhost:8080/api/v1/admin/users \
  -H "Content-Type: application/json" \
  -H "X-Admin-Api-Key: $LICENSE_ADMIN_API_KEY" \
  -d '{"name":"Acme Admin","email":"admin@acme.test","role":"CUSTOMER"}'

curl -X POST http://localhost:8080/api/v1/admin/organizations \
  -H "Content-Type: application/json" \
  -H "X-Admin-Api-Key: $LICENSE_ADMIN_API_KEY" \
  -d '{"name":"Acme Inc","email":"billing@acme.test","domain":"acme.test"}'

curl -X POST http://localhost:8080/api/v1/admin/organizations/1/memberships \
  -H "Content-Type: application/json" \
  -H "X-Admin-Api-Key: $LICENSE_ADMIN_API_KEY" \
  -d '{"userId":1,"organizationId":1,"role":"OWNER"}'

curl -X POST http://localhost:8080/api/v1/admin/products \
  -H "Content-Type: application/json" \
  -H "X-Admin-Api-Key: $LICENSE_ADMIN_API_KEY" \
  -d '{"organizationId":1,"code":"desktop-app","name":"Desktop App","description":"Main product"}'
```

Create an entitlement:

```bash
curl -X POST http://localhost:8080/api/v1/admin/products/1/entitlements \
  -H "Content-Type: application/json" \
  -H "X-Admin-Api-Key: $LICENSE_ADMIN_API_KEY" \
  -d '{"code":"feature.reports","name":"Reports"}'
```

Create a policy:

```bash
curl -X POST http://localhost:8080/api/v1/admin/policies \
  -H "Content-Type: application/json" \
  -H "X-Admin-Api-Key: $LICENSE_ADMIN_API_KEY" \
  -d '{
    "productId": 1,
    "code": "desktop-floating",
    "name": "Desktop Floating",
    "licensingModel": "FLOATING",
    "maxMachines": 5,
    "maxSeats": 3,
    "validityDays": 365,
    "heartbeatIntervalMinutes": 60,
    "heartbeatGracePeriodMinutes": 180,
    "offlineTtlDays": 7,
    "minVersion": "1.0.0",
    "entitlementCodes": ["feature.reports"]
  }'
```

Issue a license:

```bash
curl -X POST http://localhost:8080/api/v1/admin/licenses \
  -H "Content-Type: application/json" \
  -H "X-Admin-Api-Key: $LICENSE_ADMIN_API_KEY" \
  -d '{
    "userId": 1,
    "organizationId": 1,
    "policyId": 1,
    "customerName": "Acme Inc",
    "customerEmail": "admin@acme.test",
    "applicationName": "Desktop App",
    "metadata": {"order": "SO-1001"}
  }'
```

Change lifecycle status:

```bash
curl -X PATCH http://localhost:8080/api/v1/admin/licenses/1/status \
  -H "Content-Type: application/json" \
  -H "X-Admin-Api-Key: $LICENSE_ADMIN_API_KEY" \
  -d '{"status":"SUSPENDED"}'
```

Useful reads:

- `GET /api/v1/admin/users`
- `GET /api/v1/admin/organizations`
- `GET /api/v1/admin/organizations/{organizationId}/memberships`
- `GET /api/v1/admin/organizations/{organizationId}/licenses`
- `GET /api/v1/admin/products`
- `GET /api/v1/admin/policies`
- `GET /api/v1/admin/licenses`
- `GET /api/v1/admin/licenses/{licenseId}/machines`
- `GET /api/v1/admin/audit-events`

Create a runtime client token:

```bash
curl -X POST http://localhost:8080/api/v1/admin/client-tokens \
  -H "Content-Type: application/json" \
  -H "X-Admin-Api-Key: $LICENSE_ADMIN_API_KEY" \
  -d '{"name":"desktop-runtime","productId":1,"scopes":["LICENSE_VALIDATE","MACHINE_ACTIVATE","MACHINE_HEARTBEAT"]}'
```

Rotate or revoke a runtime client token:

```bash
curl -X POST http://localhost:8080/api/v1/admin/client-tokens/1/rotate \
  -H "Content-Type: application/json" \
  -H "X-Admin-Api-Key: $LICENSE_ADMIN_API_KEY" \
  -d '{"scopes":["LICENSE_VALIDATE","MACHINE_HEARTBEAT"]}'

curl -X POST http://localhost:8080/api/v1/admin/client-tokens/1/revoke \
  -H "X-Admin-Api-Key: $LICENSE_ADMIN_API_KEY"
```

Create billing plan and subscription records:

```bash
curl -X POST http://localhost:8080/api/v1/admin/billing/plans \
  -H "Content-Type: application/json" \
  -H "X-Admin-Api-Key: $LICENSE_ADMIN_API_KEY" \
  -d '{"code":"team-monthly","name":"Team Monthly","policyId":1,"amountCents":4900,"currency":"USD","billingInterval":"MONTHLY","provider":"INTERNAL"}'

curl -X POST http://localhost:8080/api/v1/admin/billing/subscriptions \
  -H "Content-Type: application/json" \
  -H "X-Admin-Api-Key: $LICENSE_ADMIN_API_KEY" \
  -d '{"organizationId":1,"planId":1,"status":"ACTIVE","provider":"INTERNAL"}'
```

## Runtime API

Runtime endpoints accept either legacy client-token auth:

```http
X-License-Client-Key: <client token returned once by /api/v1/admin/client-tokens>
```

or OAuth2-style bearer auth:

```http
Authorization: Bearer <client token returned once by /api/v1/admin/client-tokens>
```

Each endpoint enforces the relevant token scope, such as `LICENSE_VALIDATE`, `MACHINE_ACTIVATE`, `MACHINE_HEARTBEAT`, `MACHINE_DEACTIVATE`, `OFFLINE_CHECKOUT`, `OFFLINE_VERIFY`, or `OFFLINE_PUBLIC_KEY`.

Validate a license:

```bash
curl -X POST http://localhost:8080/api/v1/runtime/licenses/validate \
  -H "Content-Type: application/json" \
  -H "X-License-Client-Key: $LICENSE_CLIENT_KEY" \
  -d '{
    "key": "lic_xxx",
    "productCode": "desktop-app",
    "policyCode": "desktop-floating",
    "fingerprint": "host:serial:disk",
    "version": "1.2.0"
  }'
```

Activate a machine:

```bash
curl -X POST http://localhost:8080/api/v1/runtime/licenses/lic_xxx/machines \
  -H "Content-Type: application/json" \
  -H "X-License-Client-Key: $LICENSE_CLIENT_KEY" \
  -d '{"fingerprint":"host:serial:disk","name":"Build Agent 1","platform":"linux","version":"1.2.0"}'
```

Heartbeat/check-in:

```bash
curl -X POST http://localhost:8080/api/v1/runtime/licenses/lic_xxx/machines/heartbeat \
  -H "Content-Type: application/json" \
  -H "X-License-Client-Key: $LICENSE_CLIENT_KEY" \
  -d '{"fingerprint":"host:serial:disk","version":"1.2.1"}'
```

Deactivate:

```bash
curl -X DELETE http://localhost:8080/api/v1/runtime/licenses/lic_xxx/machines \
  -H "Content-Type: application/json" \
  -H "X-License-Client-Key: $LICENSE_CLIENT_KEY" \
  -d '{"fingerprint":"host:serial:disk"}'
```

Checkout an offline artifact:

```bash
curl -X POST http://localhost:8080/api/v1/runtime/offline/checkouts \
  -H "Content-Type: application/json" \
  -H "X-License-Client-Key: $LICENSE_CLIENT_KEY" \
  -d '{"key":"lic_xxx","fingerprint":"host:serial:disk","ttlDays":3}'
```

Verify an offline artifact:

```bash
curl -X POST http://localhost:8080/api/v1/runtime/offline/verify \
  -H "Content-Type: application/json" \
  -H "X-License-Client-Key: $LICENSE_CLIENT_KEY" \
  -d '{"artifact":"<payload.signature>"}'
```

Fetch the public verification key:

```bash
curl -X GET http://localhost:8080/api/v1/runtime/offline/public-key \
  -H "X-License-Client-Key: $LICENSE_CLIENT_KEY"
```

## Architecture

- Controllers: browser dashboard, operations console, admin API, runtime API.
- Services: license generation compatibility service, platform licensing service, billing service, scheduler, audit service, crypto service, admin auth service, client token service, rate-limit service, email service.
- Data: Flyway migrations plus JPA entities for users, organizations, memberships, products, policies, entitlements, licenses, machines, offline artifacts, scoped client API tokens, billing plans, billing subscriptions, and audit events.
- Validation flow: request -> license lookup -> status/expiry/product/policy/version checks -> optional fingerprint machine check -> entitlement and heartbeat response.
- Admin authorization flow: admin API key -> optional actor lookup -> global user role or organization membership permission check -> service action.
- Offline flow: active license plus active machine -> Ed25519 signed artifact -> local verification with public key, signature, and TTL.
- Scheduler flow: fixed-delay jobs expire licenses, mark missed heartbeats, deactivate stale missed machines, and expire subscriptions with audit events.

## Operations

- Health: `GET /actuator/health`, `GET /actuator/health/liveness`, `GET /actuator/health/readiness`
- Metrics: `GET /actuator/prometheus`
- CLI: `cli/license-manager.sh`
- Java SDK starter: `sdk/java/LicenseManagerClient.java`
- Kubernetes manifests: `deploy/kubernetes/`
- Backup/restore helpers: `scripts/backup-postgres.sh`, `scripts/restore-postgres.sh`
- Daily keep-alive ping: `.github/workflows/heart-beat.yml`

## Keep-Alive Ping

The GitHub Actions workflow `.github/workflows/heart-beat.yml` runs once per day and pings the configured endpoint.

Configure this repository secret:

```text
HEARTBEAT_URL=https://your-domain.example.com/actuator/health/readiness
```

For Supabase keep-alive, set `HEARTBEAT_URL` to the Supabase REST endpoint and add `SUPABASE_ANON_KEY`:

```text
HEARTBEAT_URL=https://your-project.supabase.co/rest/v1/
SUPABASE_ANON_KEY=<anon-key>
```

The workflow requires `https://`, retries transient failures, can be run manually, and accepts a manual URL override for one-off checks.

## Tests

```bash
mvn clean test
```

Current tests cover legacy license generation, platform issuance/validation, activation/deactivation behavior, floating seat enforcement, heartbeat reclamation, offline artifact verification, version policy bounds, organization RBAC enforcement, admin API key checks, scoped client-token rotation/revocation, billing service behavior, scheduled lifecycle sweeps, admin console rendering, Flyway schema migration, Ed25519 signing, and runtime rate limiting. JaCoCo enforces service package coverage during `mvn clean test`.

Redis integration is opt-in:

```bash
export LICENSE_RATE_LIMIT_REDIS_URL='<redis-or-rediss-url>'
mvn -Dtest=RedisRateLimitServiceTests test
```

## Trade-offs

- Admin API authorization still supports an API key header for automation. Send `X-Actor-User-Id` when requests need to be evaluated against the full user/org RBAC matrix.
- Scoped opaque runtime tokens provide OAuth2-compatible semantics but do not make this application a full authorization server.
- Runtime client tokens are stored hashed and returned only once. Rotate them if the raw token is lost.
- Redis rate limiting is production-supported through `LICENSE_RATE_LIMIT_REDIS_URL`; standard Redis uses Lettuce and Upstash URLs use Upstash's authenticated HTTPS command API. The in-memory limiter remains a local fallback when Redis is not configured.
- Billing is provider-neutral today. Stripe/Paddle/etc. should be integrated as adapters that update the internal subscription model through signed webhooks and idempotency keys.
