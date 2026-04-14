# Enterprise Architecture Plan

## Bounded Contexts
- Licensing: products, policies, licenses, machines, heartbeats, offline artifacts.
- Identity: users, organizations, memberships, permissions, future invitations and SCIM groups.
- Runtime Access: scoped client tokens and future OAuth2/OIDC token exchange.
- Billing: billing plans, subscriptions, provider references, future invoices and webhooks.
- Operations: audit events, scheduled jobs, health, metrics, logs, backups, API governance.
- Admin Console: read-oriented operational views over the same domain repositories, with write workflows routed through the governed admin API.

## Data Flow
- Admin automation authenticates with `X-Admin-Api-Key` and optionally authorizes as `X-Actor-User-Id`.
- Runtime clients authenticate with `X-License-Client-Key`; each endpoint requires a `RuntimeTokenScope`.
- License validation checks license state, expiry, product/policy compatibility, version bounds, machine fingerprint, seats, and heartbeat state.
- Scheduled jobs apply lifecycle transitions independent of request traffic and write audit events.
- Offline checkout signs a bounded artifact with Ed25519 for local verification.
- Admin console renders account, lifecycle, machine, token, billing, and audit views for human triage while preserving API-first mutation paths.

## Security Direction
- Keep backward-compatible opaque tokens while introducing OAuth2-compatible semantics: scopes, expiry, rotation, revocation, last-used tracking.
- Prefer external enterprise IdP integration for SAML/OIDC rather than embedding identity-provider responsibilities.
- Consider PostgreSQL RLS for tenant isolation if direct database access by analysts/operators becomes required.

## Billing Direction
- Keep provider-neutral internal records as source of truth for licensing entitlements.
- Treat Stripe, Paddle, or another provider as adapters that feed subscription state and invoice events into the internal billing model.
