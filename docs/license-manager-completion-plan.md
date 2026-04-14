# License Manager Completion Plan

## Findings
- Current stack: Java 21, Spring Boot 4.0.2, Maven, Spring MVC, Spring Security OAuth2 client, Thymeleaf, Spring Data JPA, PostgreSQL runtime, H2 test dependency.
- Existing domain: `User`, `Organization`, and `License` only.
- Existing workflows: OAuth2 login, organization creation, license generation/listing, basic `/api/v1/licenses/validate` boolean validation, async license backup email.
- Existing data/schema approach: JPA entities with Hibernate `ddl-auto`; no migration framework found before this completion pass.
- Existing admin UI: started as simple Thymeleaf pages and inventory views; now upgraded into a searchable operations console.
- Existing API gaps: no products, policies, entitlements, machine activations, lifecycle endpoints, heartbeat/check-in, offline artifacts, audit log, or admin authorization boundary.
- Existing test coverage: limited service/integration tests around license generation and basic validation.

## Decisions
- Keep the existing Maven/Spring Boot architecture and add targeted domain services instead of rewriting the application.
- Implement Keygen-like behavior as an API-first platform and make the browser admin surface useful for operational triage.
- Add Flyway migrations and use Hibernate validation for production schema drift protection.
- Use secure random opaque license keys and signed offline JSON artifacts with Ed25519 asymmetric signatures.
- Use an `X-Admin-Api-Key` header for admin API automation and OAuth2 plus stored `ADMIN` role for browser admin access.
- Add actor-scoped RBAC with `X-Actor-User-Id` and explicit organization membership roles for human/team authorization.
- Require hashed runtime client API tokens for client licensing endpoints.
- Use idempotent machine activation by `(license, fingerprint)` to support reinstalls safely.
- Use Redis for production rate limiting when configured, with local in-memory fallback only for development/test.
- Use scoped opaque runtime tokens as the immediate OAuth2-compatible machine-to-machine foundation while preserving legacy header compatibility.
- Keep billing provider-neutral internally so payment processors can be adapters instead of the licensing source of truth.

## Completed Tasks
- Created this progress plan.
- Added product, policy, entitlement, machine, audit event, and offline artifact domain models.
- Added license lifecycle statuses and licensing model enums.
- Added repositories for new platform entities.
- Added platform service for issue, validate, activate, deactivate, heartbeat, offline checkout, offline verify, and audit flows.
- Added API-key-protected admin API for users/customers, organizations, products, policies, licenses, machines, audit events, and client tokens.
- Added runtime client-token-protected runtime API.
- Added OAuth2 browser admin console guarded by the stored `ADMIN` role.
- Added Flyway initial schema migration, explicit Flyway-before-JPA startup wiring, and production `ddl-auto=validate` default.
- Added Ed25519 offline artifact signing and public-key discovery.
- Added runtime API rate limiting and hashed client API token issuance.
- Added Redis-backed runtime API rate limiting with fail-closed production configuration.
- Added automatic Upstash HTTPS command support for `.upstash.io` Redis URLs and verified it against the supplied instance.
- Added organization membership RBAC roles and permissions.
- Added admin membership endpoints plus actor-scoped permission checks across admin workflows.
- Added organization-owned products and product-scoped policy/client-token authorization.
- Reworked the browser admin console into a searchable operations console for dashboard metrics, accounts, products, policies, licenses, machines, runtime tokens, billing, and audit activity.
- Added scoped runtime token permissions, bearer-token compatibility, token rotation, revocation, last-used tracking, and audit events.
- Added provider-neutral billing plan and subscription domain models, APIs, migrations, RBAC checks, and tests.
- Added scheduled lifecycle jobs for license expiry, missed heartbeats, stale machine cleanup, and subscription expiry.
- Added Actuator health/readiness/liveness, Prometheus metrics, request correlation IDs, and production observability config.
- Added OpenAPI governance artifact, shell CLI, and Java runtime SDK starter.
- Added enterprise gap assessment, roadmap, architecture notes, deployment runbook, and security threat model.
- Added Kubernetes deployment and backup manifests plus local Postgres backup/restore scripts.
- Added explicit production profile configuration in `src/main/resources/application-prod.yml`.
- Added local demo seed data.
- Updated runtime/test configuration for secure env vars, H2 tests, seed toggles, email toggles, and SMTP aliases.
- Added tests for platform lifecycle, activation, floating seats, heartbeat reclamation, offline artifacts, version bounds, admin auth, scoped client-token lifecycle, billing service behavior, scheduler lifecycle sweeps, admin console rendering, Flyway plus Hibernate schema validation, crypto signing, and rate limiting.
- Added JaCoCo service package coverage enforcement to Maven tests.
- Updated README and final implementation summary.
- Final verification passed with `mvn clean test`.

## Implementation Tasks
- Add product, policy, entitlement, machine, audit event, and offline artifact domain models.
- Extend license lifecycle with active, inactive, suspended, expired, and revoked states.
- Add repositories and domain services for product/policy/license/machine/audit/offline flows.
- Add REST APIs for admin workflows and client validation/activation/check-in/offline verification.
- Add demo seed data for local development.
- Update test configuration to use H2 for repeatable local tests.
- Add domain and HTTP tests for lifecycle, validation, activation, seat enforcement, heartbeats, offline artifacts, and authz failures.
- Add enterprise foundation tests for scoped runtime tokens, billing, schedulers, and admin console rendering.
- Update README and add final implementation summary.

## Remaining Tasks
- Full embedded OAuth2/OIDC authorization server is not implemented; current runtime tokens are scoped opaque tokens with bearer compatibility.
- Full SAML/SCIM and invitation/approval workflows are planned but require IdP/provisioning decisions.
- Payment collection and provider webhook processing remain adapter work on top of the internal billing model.
- PostgreSQL row-level security and distributed scheduler locks are documented hardening steps for larger enterprise deployments.

## Blockers Resolved
- Maven needed external dependency download for Spring Boot JSON support.
- H2 exposed reserved SQL identifier usage for `License.key`; physical column renamed to `license_key`.
- Async email failures were caught and made configurable with `license.email.enabled`.
- Spring Boot 4 did not auto-run Flyway from the plain Flyway dependency in this project, so `FlywaySchemaConfig` now migrates before JPA validation.
- JPA validation exposed a migration/model nullability mismatch on `policy_entitlements.entitlement_code`; the entity mapping now matches the schema.
- Redis URL handling now respects the URI scheme instead of forcing TLS for provider hostnames.
- Upstash provider handling now uses the provider's HTTPS command API, resolving the supplied instance verification failure.
