# Enterprise Roadmap

## Phase 1: Foundations Completed In This Pass
- Scoped runtime tokens with rotation, revocation, last-used tracking, and per-endpoint enforcement.
- Searchable operations console for accounts, products, lifecycle state, machines, tokens, billing, and audit activity.
- Provider-agnostic billing plans and subscriptions with organization RBAC.
- Scheduled jobs for license expiry, missed heartbeats, stale machine cleanup, and subscription expiry.
- Actuator health/readiness, Prometheus metrics exposure, and request correlation IDs.
- OpenAPI artifact, CLI starter, and Java SDK starter.
- Threat model, deployment runbook, and operational documentation.
- Kubernetes deployment and backup CronJob manifests plus local backup/restore scripts.

## Phase 2: Enterprise Identity
- Add SAML service-provider support through Spring Security SAML or a dedicated identity broker.
- Add SCIM 2.0 endpoints for users and group-to-role membership provisioning.
- Add invitation and approval workflow tables: invitations, approval requests, approval decisions.
- Add admin UI flows for inviting users, approving role changes, and viewing IAM audit events.

## Phase 3: API Governance
- Add cursor pagination, filtering, sorting, and consistent envelopes to list endpoints.
- Split OpenAPI into admin and runtime specs and generate SDK clients in CI.
- Add deprecation headers and compatibility tests for `/api/v1`.

## Phase 4: Billing Provider Adapters
- Implement Stripe adapter behind the billing service.
- Add webhook signature verification, idempotency keys, invoice events, and payment-failure state transitions.
- Add subscription-to-license automation for entitlement and seat changes.

## Phase 5: Operations
- Convert Kubernetes manifests into a Helm chart or Kustomize overlays for the chosen deployment platform.
- Add restore drill CI task against an isolated PostgreSQL instance.
- Add Grafana dashboard JSON and alert rules for Prometheus/Alertmanager.
- Add load-test baselines and capacity targets.
