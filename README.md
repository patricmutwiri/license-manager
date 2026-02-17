#  License Manager & Validator

A robust, web-based license generation system built with **Spring Boot 3**. This project allows users to log in via **Google** or **GitHub**, generate signed license keys with expiration dates, and provides a REST API for remote software verification.

---

##  Features

- ** Social Authentication:** Integrated with Google and GitHub OAuth2 via [Spring Security](https://spring.io).
- ** License Generation:** Secure SHA-256 hashing using a server-side secret salt.
- ** Expiration Control:** Licenses are generated with a customizable 1-year validity period.
- ** Database Persistence:** Stores issued keys and user metadata using [Spring Data JPA](https://spring.io).
- ** Validation API:** A `/api/v1/license/verify` REST endpoint for external software to validate keys.
- ** Responsive Dashboard:** A clean [Thymeleaf](https://www.thymeleaf.org) + Bootstrap frontend for customers.

---

## Tech Stack

- **Backend:** Java 17+, Spring Boot 3.x
- **Security:** OAuth2, SHA-256 Hashing
- **Database:** H2 (In-memory for testing) or MySQL/PostgreSQL
- **Frontend:** Thymeleaf, Bootstrap 5, JavaScript

---

## Getting Started

### 1. Prerequisites
- **Java 17** or higher.
- **Maven** 3.8+.
- **OAuth Credentials:** 
  - Create a project in [Google Cloud Console](https://console.cloud.google.com).
  - Create an OAuth App in [GitHub Developer Settings](https://github.com).

### 2. Configure Environment Variables
Do not hardcode secrets. Set these in your environment or `application.yml`:
```yaml
GOOGLE_CLIENT_ID: your_google_id
GOOGLE_CLIENT_SECRET: your_google_secret
GITHUB_CLIENT_ID: your_github_id
GITHUB_CLIENT_SECRET: your_github_secret


