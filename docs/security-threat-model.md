# Security Threat Model

## Assets
- License keys, machine fingerprints, offline signing keys, runtime client tokens, admin API key, customer data, audit log integrity, billing subscription state.

## Primary Threats
- Runtime token theft and overbroad client permissions.
- License replay across machines.
- Offline artifact tampering.
- Tenant data leakage through admin APIs.
- Brute force validation attempts.
- Scheduler or webhook replay causing incorrect lifecycle transitions.
- Secret leakage through logs or repository files.

## Controls Implemented
- Runtime tokens are hashed at rest and returned once.
- Runtime tokens have scopes, expiry, rotation, revocation, and last-used timestamps.
- Runtime endpoints are rate-limited with Redis/Upstash support.
- Machine validation is fingerprint-scoped.
- Offline artifacts are signed with Ed25519 and have TTLs.
- Admin operations require an API key and can enforce actor-scoped RBAC.
- Production error responses suppress stack traces.
- Request correlation IDs are added to every request/response.

## Pentest Readiness Checklist
- Verify no real secrets are in git history or deployed images.
- Attempt cross-organization access using non-owner actors.
- Attempt runtime endpoints with missing, expired, revoked, or underscoped tokens.
- Fuzz license keys, fingerprints, and offline artifacts.
- Validate Redis limiter behavior under burst traffic.
- Validate scheduler idempotency and audit records.
- Validate backup restore and offline signing key handling.

## Remaining Security Architecture Decisions
- Whether to adopt PostgreSQL row-level security.
- Whether M2M OAuth tokens come from an external IdP or an embedded authorization server.
- Which SAML/SCIM provider strategy to standardize on.
