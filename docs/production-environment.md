# Production Environment

Use environment variables or your deployment secret manager. Do not commit real values.

## Required

```bash
SPRING_PROFILES_ACTIVE=prod
PORT=8080

SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/license_manager
SPRING_DATASOURCE_USERNAME=<database-user>
SPRING_DATASOURCE_PASSWORD=<database-password>
SPRING_FLYWAY_ENABLED=true
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_JPA_SHOW_SQL=false
SPRING_JPA_OPEN_IN_VIEW=false

LICENSE_ADMIN_API_KEY=<long-random-admin-api-key>
LICENSE_OFFLINE_PRIVATE_KEY_BASE64=<pkcs8-ed25519-private-key>
LICENSE_OFFLINE_PUBLIC_KEY_BASE64=<x509-ed25519-public-key>
LICENSE_RATE_LIMIT_REDIS_URL=<redis-or-rediss-url>
LICENSE_RATE_LIMIT_REDIS_KEY_PREFIX=license-manager:rate-limit
LICENSE_RATE_LIMIT_RUNTIME_PER_MINUTE=120
LICENSE_RATE_LIMIT_FAIL_OPEN=false

SMTP_HOST=<smtp-host>
SMTP_PORT=465
SMTP_USER=<smtp-user>
SMTP_PASS=<smtp-password>
SMTP_AUTH=true
SMTP_TLS=true
SMTP_MAIL_FROM=<verified-from-address>

SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=<google-client-id>
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=<google-client-secret>
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_ID=<github-client-id>
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_SECRET=<github-client-secret>
```

## Security Defaults

- Runtime licensing endpoints require `X-License-Client-Key`.
- Admin automation endpoints require `X-Admin-Api-Key`.
- Actor-scoped admin authorization can include `X-Actor-User-Id`; when present, organization membership RBAC is enforced.
- Production profile is configured in `src/main/resources/application-prod.yml` and fails startup when required secrets are missing.
- Redis rate limiting should run fail-closed in production with `LICENSE_RATE_LIMIT_FAIL_OPEN=false`.
- Offline license files should use stable Ed25519 keys. Ephemeral generated keys are only acceptable for local development.

## RBAC Matrix

| Role | Scope | Main Permissions |
| --- | --- | --- |
| `ADMIN` | Global user role | All platform administration when used as an actor |
| `OWNER` | Organization membership | All organization permissions |
| `ADMIN` | Organization membership | Organization, membership, product, policy, license, machine, audit, and client-token management |
| `BILLING` | Organization membership | Organization read, license read, audit read |
| `DEVELOPER` | Organization membership | Organization read, license read, machine read, client-token management |
| `SUPPORT` | Organization membership | Organization read, license read/update, machine read, audit read |
| `VIEWER` | Organization membership | Organization read, license read, machine read, audit read |

## Test Redis

For local integration testing, export `LICENSE_RATE_LIMIT_REDIS_URL` in the shell before running Maven. The test skips when the variable is absent so CI is not coupled to a single external Redis instance.

The Redis URI scheme is honored exactly. Use `rediss://` for TLS endpoints and `redis://` for plaintext endpoints.
