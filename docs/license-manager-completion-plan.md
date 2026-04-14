# License Manager Completion Plan

## Findings
- Current stack: Java 21, Spring Boot 4.0.2, Maven, Spring MVC, Spring Security OAuth2 client, Thymeleaf, Spring Data JPA, PostgreSQL runtime, H2 test dependency.
- Existing domain: `User`, `Organization`, and `License` only.
- Existing workflows: OAuth2 login, organization creation, license generation/listing, basic `/api/v1/licenses/validate` boolean validation, async license backup email.
- Existing data/schema approach: JPA entities with Hibernate `ddl-auto`; no migration framework found before this completion pass.
- Existing admin UI: simple Thymeleaf pages for organizations and license generation/listing.
- Existing API gaps: no products, policies, entitlements, machine activations, lifecycle endpoints, heartbeat/check-in, offline artifacts, audit log, or admin authorization boundary.
- Existing test coverage: limited service/integration tests around license generation and basic validation.

## Decisions
- Keep the existing Maven/Spring Boot architecture and add targeted domain services instead of rewriting the application.
- Implement Keygen-like behavior as an API-first platform and preserve the current dashboard as a lightweight admin surface.
- Add Flyway migrations and use Hibernate validation for production schema drift protection.
- Use secure random opaque license keys and signed offline JSON artifacts with Ed25519 asymmetric signatures.
- Use an `X-Admin-Api-Key` header for admin API automation and OAuth2 plus stored `ADMIN` role for browser admin access.
- Require hashed runtime client API tokens for client licensing endpoints.
- Use idempotent machine activation by `(license, fingerprint)` to support reinstalls safely.

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
- Added local demo seed data.
- Updated runtime/test configuration for secure env vars, H2 tests, seed toggles, email toggles, and SMTP aliases.
- Added tests for platform lifecycle, activation, floating seats, heartbeat reclamation, offline artifacts, version bounds, admin auth, Flyway plus Hibernate schema validation, crypto signing, and rate limiting.
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
- Update README and add final implementation summary.

## Remaining Tasks
- Commit pending.

## Blockers Resolved
- Maven needed external dependency download for Spring Boot JSON support.
- H2 exposed reserved SQL identifier usage for `License.key`; physical column renamed to `license_key`.
- Async email failures were caught and made configurable with `license.email.enabled`.
- Spring Boot 4 did not auto-run Flyway from the plain Flyway dependency in this project, so `FlywaySchemaConfig` now migrates before JPA validation.
- JPA validation exposed a migration/model nullability mismatch on `policy_entitlements.entitlement_code`; the entity mapping now matches the schema.
