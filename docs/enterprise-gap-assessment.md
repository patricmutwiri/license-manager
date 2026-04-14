# Enterprise Gap Assessment

## Current Architecture
- Java 21, Maven, Spring Boot 4, Spring MVC, Spring Security OAuth2 client, Thymeleaf, Spring Data JPA, Flyway, PostgreSQL runtime, H2 tests.
- Core licensing domain exists: products, policies, entitlements, licenses, machines, offline artifacts, users, organizations, memberships, client tokens, audit events.
- Runtime API supports validation, activation, heartbeat, deactivation, offline checkout, offline verification, and public key discovery.
- Admin API supports users, organizations, memberships, products, policies, licenses, machines, audit events, client tokens, and billing foundations.

## Gap Analysis
- Admin UI: searchable operations console exists for dashboard signals, accounts, product/policy views, license lifecycle, machine heartbeat state, runtime tokens, billing, and audit activity; write workflows remain API-first.
- Authentication: runtime tokens now support scopes, rotation, revocation, last-used tracking, and audit events; full OAuth2 authorization-server issuance is not embedded.
- IAM: organization membership RBAC exists. SSO login exists through OAuth2 clients. SAML and SCIM require external IdP/provider integration and are documented as phased work.
- Billing: provider-agnostic plan/subscription domain exists with Stripe-ready provider fields. Webhook processing and payment collection are intentionally provider adapters.
- Deployment: production profile, actuator health probes, Prometheus metrics, Dockerfile, Kubernetes manifests, backup CronJob, backup/restore scripts, and runbooks exist.
- Observability: request correlation, actuator metrics, Prometheus endpoint, audit events, and scheduler logs exist. Full dashboards and alert routing belong in deployment-specific infrastructure.
- Background jobs: scheduler now covers expiry, heartbeat cleanup, stale machine cleanup, and subscription expiry.
- SDK/CLI: repository includes a practical CLI and Java runtime SDK starter.
- API governance: OpenAPI document exists for public/admin APIs. Pagination/filtering standardization is still a future compatibility task for large datasets.
- Security: API keys and runtime tokens are hashed. Runtime tokens are scoped. Redis rate limiting supports Upstash and standard Redis. Threat model and pentest checklist exist.

## Highest-Risk Areas
- Browser admin write workflows are intentionally limited because API-key and actor-header flows are clearer for production automation today.
- OAuth2/OIDC-compatible M2M is approximated with scoped opaque tokens. A full authorization server or external issuer integration remains a larger architecture decision.
- Tenant isolation is enforced in service/RBAC logic; database row-level security can be added if PostgreSQL RLS is selected as an enterprise requirement.
- Payment collection and provider webhook ingestion are not implemented yet; the billing domain is ready for adapter integration.
- Full SAML/SCIM enterprise IAM requires selecting IdP integration contracts before completing implementation.
