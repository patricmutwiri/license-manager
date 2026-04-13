# License Manager App Summary

## What It Is
License Manager is a Spring Boot web app for creating organizations, issuing expiring software license keys, and validating licenses through a REST API. It includes an OAuth2-backed dashboard and email backup flow for generated licenses.

## Who It’s For
Software/product teams or customer admins who need to manage organization-based license distribution. A more specific persona is not found in repo.

## What It Does
- Authenticates users with Google or GitHub OAuth2.
- Creates and lists customer organizations with name, email, and domain.
- Generates UUID license keys for a user and organization.
- Stores license metadata: application name, hostname, backup email, expiry date, active flag, and custom fields.
- Lists licenses by organization in the Thymeleaf dashboard.
- Validates license keys via `GET /api/v1/licenses/validate?key=...`.
- Sends generated license details by async backup email.

## How It Works
- **UI:** `IndexController` serves Thymeleaf pages for login, organization management, license generation, and license listing.
- **API:** `LicenseController` exposes `/api/v1/licenses` for generation, validation, and lookup by user or organization; `OrganizationController` exposes `/api/organizations`.
- **Auth:** `SecurityConfig` enables OAuth2 login and CSRF cookies, while `UserService` persists OAuth users from provider attributes.
- **Domain:** JPA entities model `User`, `Organization`, and `License`; licenses belong to both a user and an organization.
- **Services:** `LicenseServiceImpl` validates user/org existence, creates the license, saves it through Spring Data repositories, and triggers `EmailService.sendLicenseBackupAsync`.
- **Data flow:** Browser/API request -> controller -> service -> repository -> PostgreSQL; generated licenses also flow to the async mail service.

## How To Run
1. Install Java 21 and use the included Maven wrapper.
2. Start PostgreSQL and create a `license_manager` database. Database bootstrap SQL is not found in repo.
3. Set environment variables from `.env.example`, especially PostgreSQL, Google/GitHub OAuth2, SMTP, and `PORT`.
4. Run: `./mvnw spring-boot:run`
5. Open: `http://localhost:8080`

