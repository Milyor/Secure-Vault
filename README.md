# Secure Vault

Secure Vault is a Spring Boot document storage API built with Java 17. It exposes a small authenticated REST surface for uploading files, listing the current user’s files, downloading a file by UUID, and reading the current account. File bytes are stored in S3, file metadata is stored in PostgreSQL, and users are stored in a separate PostgreSQL database.

## What it uses

- Java 17
- Spring Boot 4.0.2
- Spring Web MVC
- Spring Security with HTTP Basic auth
- Spring Data JPA / Hibernate
- PostgreSQL
- AWS SDK for S3
- LocalStack for local S3 emulation
- Docker Compose for local infrastructure
- Lombok
- dotenv-java for environment variable loading
- Spring Boot Actuator
- JUnit 5, Mockito, AssertJ, and Spring MVC test support

## Runtime layout

The application is split across two databases and one object store:

- `files` database: stores `FileDocument` records, including the owner id, original filename, content type, size, S3 object key, and whether the object is gzip-compressed
- `users` database: stores `Users` records with username, hashed password, and role
- S3 bucket: stores the actual file bytes

Uploads are compressed before storage when the content type is considered text-like by `CompressionPolicy`. Downloads reverse that compression when needed.

## API

All endpoints require authentication.

- `POST /upload-file` uploads a multipart file using the form field name `file`
- `GET /files` returns the authenticated user’s files with id, name, size, content type, and download URL
- `GET /download/{id}` streams a file back to the caller
- `GET /account/me` returns the authenticated user’s id, username, and role

## Project structure

```text
src/main/java/io/github/milyor/doc_storage_api/
├── config/       # security, S3, database wiring, startup seeding
├── controller/   # REST endpoints
├── service/      # file storage, user lookup, compression policy
├── repository/   # Spring Data repositories
├── model/        # JPA entities and authenticated principal wrapper
└── dto/          # response DTOs

src/test/java/io/github/milyor/doc_storage_api/
├── controller/   # WebMvc slice tests for authenticated endpoints
└── service/      # unit tests for file storage and compression behavior
```

## Configuration

The application reads its runtime settings from environment variables. The main ones are:

- `DB_NAME`, `DB_USER`, `DB_PASSWORD` for the files database
- `USER_DB_NAME`, `USER_DB_USER`, `USER_DB_PASSWORD` for the users database
- `AWS_REGION`, `AWS_S3_BUCKET`, `AWS_S3_ENDPOINT`, `AWS_S3_PATH_STYLE` for S3
- `SEED_USER_NAME`, `SEED_USER_PASSWORD`, `SEED_USER_ROLE` for the default account

For local development, the app expects:

- PostgreSQL on ports `5433` and `5434`
- LocalStack S3 on port `4566`

The bundled LocalStack init script creates the bucket on startup so uploads work immediately.

## Running locally

1. Create a `.env` file in the project root with values for the database and S3 variables.
2. Start the app:

```bash
./gradlew bootRun
```

3. Run the tests:

```bash
./gradlew test
```

Spring Boot’s Docker Compose support starts the local PostgreSQL containers automatically. If you are using LocalStack for S3, set `AWS_S3_ENDPOINT=http://localhost:4566`.

## Testing

Test coverage is currently split between controller slice tests and service unit tests:

- `FileManagerControllerTest` checks authentication enforcement, successful upload, upload failure handling, owner-scoped file listing, download streaming, and the 404 path for non-owned files
- `SecurityControllerTest` checks that `/account/me` is protected and returns the current user payload
- `FileStorageServiceTest` checks raw uploads, gzip compression for compressible content, decompression on download, owner lookup, and not-found behavior
- `CompressionPolicyTest` checks which content types are compressed and how null or parameterized values are handled

The controller tests use `@WebMvcTest` with the real security configuration imported, while the service tests use Mockito-backed unit tests.

## Security notes

- Authentication uses HTTP Basic
- Sessions are stateless
- Passwords are stored as BCrypt hashes
- File ownership is enforced by UUID owner id
- Non-owned downloads return `404` rather than `403` to avoid revealing file existence
- CSRF is disabled in the current security configuration

## Seeded account

On startup, `DataSeeder` creates a default user if it does not already exist. The default credentials come from environment variables and fall back to:

- `SEED_USER_NAME=admin`
- `SEED_USER_PASSWORD=changeme`
- `SEED_USER_ROLE=ROLE_ADMIN`

