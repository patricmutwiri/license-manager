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
- OAuth2-backed Thymeleaf dashboard plus an admin console for platform inventory.
- API-first admin/runtime flows with JSON responses.
- Flyway-managed schema, runtime client API tokens, rate limiting, and service coverage enforcement.

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

The platform inventory console is available at `http://localhost:8080/admin` for authenticated users whose stored role is `ADMIN`. It shows organizations, team memberships, products, policies, licenses, machines, and audit events; write workflows are exposed through the admin REST API.

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
| `ADMIN` | Organization, membership, product, policy, license, machine, audit, and client-token management |
| `BILLING` | Organization read, license read, audit read |
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
  -d '{"name":"desktop-runtime","productId":1}'
```

## Runtime API

All runtime endpoints require:

```http
X-License-Client-Key: <client token returned once by /api/v1/admin/client-tokens>
```

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

- Controllers: browser dashboard, admin console, admin API, runtime API.
- Services: license generation compatibility service, platform licensing service, audit service, crypto service, admin auth service, client token service, rate-limit service, email service.
- Data: Flyway migrations plus JPA entities for users, organizations, products, policies, entitlements, licenses, machines, offline artifacts, client API tokens, and audit events.
- Validation flow: request -> license lookup -> status/expiry/product/policy/version checks -> optional fingerprint machine check -> entitlement and heartbeat response.
- Admin authorization flow: admin API key -> optional actor lookup -> global user role or organization membership permission check -> service action.
- Offline flow: active license plus active machine -> Ed25519 signed artifact -> local verification with public key, signature, and TTL.

## Tests

```bash
mvn clean test
```

Current tests cover legacy license generation, platform issuance/validation, activation/deactivation behavior, floating seat enforcement, heartbeat reclamation, offline artifact verification, version policy bounds, organization RBAC enforcement, admin API key checks, Flyway schema migration, Ed25519 signing, and runtime rate limiting. JaCoCo enforces service package coverage during `mvn clean test`.

Redis integration is opt-in:

```bash
export LICENSE_RATE_LIMIT_REDIS_URL='<redis-or-rediss-url>'
mvn -Dtest=RedisRateLimitServiceTests test
```

## Trade-offs

- Admin API authorization still supports an API key header for automation. Send `X-Actor-User-Id` when requests need to be evaluated against the full user/org RBAC matrix.
- Runtime client tokens are stored hashed and returned only once. Rotate them if the raw token is lost.
- Redis rate limiting is production-supported through `LICENSE_RATE_LIMIT_REDIS_URL`; standard Redis uses Lettuce and Upstash URLs use Upstash's authenticated HTTPS command API. The in-memory limiter remains a local fallback when Redis is not configured.
