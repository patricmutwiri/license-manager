# License Manager

Production-oriented licensing platform built with Java 21, Spring Boot 4, Maven, Spring MVC, Spring Security OAuth2, Thymeleaf, and Spring Data JPA.

The platform now supports Keygen-like concepts: products, policies, licenses, machines, feature entitlements, lifecycle states, activation limits, machine heartbeats, signed offline artifacts, audit events, and admin/runtime REST APIs.

## Features

- Product and policy-driven license issuance.
- License lifecycle: `ACTIVE`, `INACTIVE`, `SUSPENDED`, `EXPIRED`, `REVOKED`.
- Node-locked and floating/seat-based activation models.
- Fingerprint-scoped machine activation, deactivation, and validation.
- Heartbeat/check-in tracking with missed-heartbeat seat reclamation.
- HMAC-SHA256 signed offline license artifacts with TTL and local verification.
- Feature entitlement attachment at policy level.
- Audit trail for products, policies, licenses, machines, heartbeat misses, and offline checkouts.
- OAuth2-backed Thymeleaf dashboard retained for existing organization/license workflows.
- API-first admin/runtime flows with JSON responses.

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
export SPRING_JPA_HIBERNATE_DDL_AUTO='update'

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
export LICENSE_SIGNING_SECRET='replace-with-at-least-24-random-characters'
export LICENSE_SEED_ENABLED='true'
export LICENSE_EMAIL_ENABLED='true'
export PORT='8080'
```

The app also accepts the older `SMTP_MAIL_HOST`, `SMTP_MAIL_PORT`, `SMTP_MAIL_USERNAME`, `SMTP_MAIL_PASSWORD`, `SMTP_MAIL_AUTH`, and `SMTP_MAIL_TLS` names.

## Run Locally

```bash
createdb license_manager
mvn clean test
mvn spring-boot:run
```

Open the dashboard at `http://localhost:8080`.

Seed data is created when `LICENSE_SEED_ENABLED=true`: a demo user, organization, product, entitlements, floating policy, and license.

## Admin API

All admin endpoints require:

```http
X-Admin-Api-Key: <LICENSE_ADMIN_API_KEY>
```

Create a product:

```bash
curl -X POST http://localhost:8080/api/v1/admin/products \
  -H "Content-Type: application/json" \
  -H "X-Admin-Api-Key: $LICENSE_ADMIN_API_KEY" \
  -d '{"code":"desktop-app","name":"Desktop App","description":"Main product"}'
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

- `GET /api/v1/admin/products`
- `GET /api/v1/admin/policies`
- `GET /api/v1/admin/licenses`
- `GET /api/v1/admin/licenses/{licenseId}/machines`
- `GET /api/v1/admin/audit-events`

## Runtime API

Validate a license:

```bash
curl -X POST http://localhost:8080/api/v1/runtime/licenses/validate \
  -H "Content-Type: application/json" \
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
  -d '{"fingerprint":"host:serial:disk","name":"Build Agent 1","platform":"linux","version":"1.2.0"}'
```

Heartbeat/check-in:

```bash
curl -X POST http://localhost:8080/api/v1/runtime/licenses/lic_xxx/machines/heartbeat \
  -H "Content-Type: application/json" \
  -d '{"fingerprint":"host:serial:disk","version":"1.2.1"}'
```

Deactivate:

```bash
curl -X DELETE http://localhost:8080/api/v1/runtime/licenses/lic_xxx/machines \
  -H "Content-Type: application/json" \
  -d '{"fingerprint":"host:serial:disk"}'
```

Checkout an offline artifact:

```bash
curl -X POST http://localhost:8080/api/v1/runtime/offline/checkouts \
  -H "Content-Type: application/json" \
  -d '{"key":"lic_xxx","fingerprint":"host:serial:disk","ttlDays":3}'
```

Verify an offline artifact:

```bash
curl -X POST http://localhost:8080/api/v1/runtime/offline/verify \
  -H "Content-Type: application/json" \
  -d '{"artifact":"<payload.signature>"}'
```

## Architecture

- Controllers: browser dashboard, admin API, runtime API.
- Services: license generation compatibility service, platform licensing service, audit service, crypto service, admin auth service, email service.
- Data: JPA entities for users, organizations, products, policies, entitlements, licenses, machines, offline artifacts, and audit events.
- Validation flow: request -> license lookup -> status/expiry/product/policy/version checks -> optional fingerprint machine check -> entitlement and heartbeat response.
- Offline flow: active license plus active machine -> signed HMAC artifact -> local verification by signature and TTL.

## Tests

```bash
mvn clean test
```

Current tests cover legacy license generation, platform issuance/validation, activation/deactivation behavior, floating seat enforcement, heartbeat reclamation, offline artifact verification, version policy bounds, and admin API key checks.

## Trade-offs

- Schema management uses Hibernate `ddl-auto` because no migration tool exists in this repository yet.
- Admin API authorization uses an API key header. OAuth2 login remains for the dashboard, but role-based admin accounts are not modeled yet.
- Offline artifacts use HMAC signatures, so server and verifier share the signing secret. Public-key signatures would be stronger for third-party client verification.

