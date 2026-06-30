# Secure Vault — Document Storage API

A secure REST API for uploading, storing, and retrieving documents, built with **Spring Boot** and **Java 17**. Files are persisted as binary objects in PostgreSQL, every endpoint is protected by **Spring Security** with **BCrypt**-hashed credentials, and user data is isolated from file data using a **multi-database architecture** (two separate PostgreSQL instances).

## What it does

Secure Vault exposes a small, authenticated HTTP API that lets a client:

- **Upload** documents of any type (PDFs, images, etc.) up to 1 GB per file
- **List** the authenticated user's stored files with metadata and a ready-to-use download link
- **Download** one of the authenticated user's files by its UUID
- **Authenticate** every request via HTTP Basic auth and inspect the current account

The project demonstrates production-minded backend patterns: credential hashing, stateless session management, separation of concerns across controller/service/repository layers, and physical separation of sensitive user data from stored content.

## Startup seeding

At application startup, `DataSeeder` creates a default user only if that username does not already exist. This is idempotent and safe to run on every boot.

The seeded account is controlled by environment variables, with these defaults:

- `SEED_USER_NAME=admin`
- `SEED_USER_PASSWORD=changeme`
- `SEED_USER_ROLE=ROLE_ADMIN`

This keeps credentials out of the repository while still providing a usable local account for first run.

## Architecture

The application connects to **two independent PostgreSQL databases**, each with its own JPA configuration, entity manager, and transaction manager:

| Database | Purpose | Port |
|----------|---------|------|
| File DB (primary) | Stores uploaded documents as binary large objects | 5433 |
| User DB | Stores user accounts, roles, and hashed passwords | 5434 |

This isolation means a compromise or migration of the document store never touches credential data. Both databases are provisioned via Docker Compose.

```
Client ──HTTP Basic──▶ Spring Security ──▶ Controllers ──▶ Services ──▶ JPA Repositories
                                                                          │
                                          ┌───────────────────────────────┴───────────────┐
                                          ▼                                                 ▼
                                  File DB (documents)                              User DB (accounts)
```

## Tech stack

- **Java 17** / **Spring Boot 4**
- **Spring Web (MVC)** — REST endpoints
- **Spring Security** — HTTP Basic auth, BCrypt password encoding, stateless sessions
- **Spring Data JPA / Hibernate** — persistence layer
- **PostgreSQL** — dual-database storage (binary documents + user accounts)
- **Docker Compose** — local database provisioning
- **Lombok** — boilerplate reduction
- **Gradle (Kotlin DSL)** — build tool
- **Spring Boot Actuator** — health and monitoring endpoints

## API endpoints

All endpoints require authentication.

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/upload-file` | Upload a file (multipart form field `file`). Returns `true` on success. |
| `GET` | `/files` | List the current user's stored files with id, name, size, type, and download URL. |
| `GET` | `/download/{id}` | Download one of the current user's files by its UUID. |
| `GET` | `/account/me` | Return the authenticated user's id, username, and role. |

### Example

```bash
# Upload a document
curl -u user:password -F "file=@report.pdf" http://localhost:8080/upload-file

# List stored files
curl -u user:password http://localhost:8080/files

# Download by id
curl -u user:password http://localhost:8080/download/<file-uuid> -o report.pdf
```

## Getting started

### Prerequisites

- Java 17+
- Docker & Docker Compose

### 1. Configure environment

Create a `.env` file in the project root (it is git-ignored):

```env
POSTGRES_VERSION=16

# File storage database
DB_NAME=vault
DB_USER=vault_user
DB_PASSWORD=change_me

# User accounts database
USER_DB_NAME=vault_users
USER_DB_USER=user_admin
USER_DB_PASSWORD=change_me
```

### 2. Run

Spring Boot's Docker Compose support starts both PostgreSQL containers automatically:

```bash
./gradlew bootRun
```

The API will be available at `http://localhost:8080`.

To run the test suite:

```bash
./gradlew test
```

## Testing

The current test coverage is focused on the web layer:

- `FileManagerControllerTest` verifies authentication is enforced, file upload returns `true`/`false` as expected, file listing is scoped to the authenticated owner, and downloads return the file bytes or `404` when the file is not accessible.
- `SecurityControllerTest` verifies `/account/me` is protected and returns the current user's id, username, and role.

These tests run against the real Spring Security configuration with the service layer mocked.

## Security notes

- Passwords are stored only as **BCrypt** hashes; the `Users` entity excludes the password from its `toString()`.
- Sessions are **stateless** — credentials are validated on every request.
- File IDs are **UUIDs** rather than sequential integers, preventing enumeration.
- CSRF is disabled for API simplicity and should be reviewed before a production deployment.

## Project structure

```
src/main/java/io/github/milyor/doc_storage_api/
├── config/       # Security + dual-datasource configuration
├── controller/   # REST endpoints (files + account)
├── service/      # Business logic (file storage, user lookup)
├── repository/   # Spring Data JPA repositories
├── model/        # JPA entities (FileDocument, Users) + UserPrincipal
└── dto/          # Response DTOs
```
