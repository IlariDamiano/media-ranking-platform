# Development Guide

This document explains how to set up and work on the Media Ranking Platform locally.

The current development environment uses Windows 11, IntelliJ IDEA, Java 25, Docker Desktop, PostgreSQL 18, and Gradle.

## 1. Prerequisites

Install the following tools:

| Tool | Purpose |
|---|---|
| Git | Version control |
| Java JDK 25 | Backend development |
| IntelliJ IDEA | Java IDE |
| Docker Desktop | Containerized database |
| PostgreSQL database manager | Inspect and manage database data |
| PowerShell | Run development commands |

The backend includes the Gradle Wrapper, so installing Gradle separately is not necessary.

A TMDB API read access token is needed when using the live TMDB integration.

## 2. Project Structure

The repository is organized as follows:

```text
media-ranking-platform/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   │       └── db/migration/
│   │   └── test/
│   ├── gradlew.bat
│   └── build.gradle
├── frontend/
├── infrastructure/
│   └── compose.yaml
├── docs/
└── README.md
```

The backend contains the currently implemented application.

The frontend is planned for a later development phase.

## 3. Clone the Repository

Clone the repository using its actual GitHub URL:

```powershell
git clone <repository-url>
cd media-ranking-platform
```

Open the project in IntelliJ IDEA and let Gradle import the backend dependencies.

Select Java 25 as the project SDK and Gradle JVM.

## 4. Start PostgreSQL

Docker Compose is used for the local PostgreSQL environment.

From the repository root:

```powershell
cd infrastructure
docker compose up -d
```

Check the running containers:

```powershell
docker compose ps
```

The PostgreSQL container should report a running status.

To stop the infrastructure without deleting the database volume:

```powershell
docker compose down
```

Do not use `docker compose down -v` unless you intentionally want to remove the persistent database volume.

## 5. Database Configuration

The application uses two PostgreSQL databases:

| Database | Purpose |
|---|---|
| `media_ranking` | Local application development |
| `media_ranking_test` | Integration testing |

Ensure both databases exist before running database-backed tests.

Database credentials should match the credentials configured for the local PostgreSQL instance.

A PostgreSQL management application can be used to inspect schemas, query tables, and verify synchronization data.

Flyway manages database schema changes.

Existing applied migration files should not be modified. Add a new numbered migration for future schema changes.

## 6. Environment Variables

The backend uses environment variables for sensitive configuration.

Required variables include:

| Variable | Purpose |
|---|---|
| `DB_USERNAME` | PostgreSQL application username |
| `DB_PASSWORD` | PostgreSQL application password |
| `TMDB_ACCESS_TOKEN` | TMDB API read access token |

Example PowerShell session:

```powershell
$env:DB_USERNAME = "media_app"
$env:DB_PASSWORD = Read-Host "Database password"
$env:TMDB_ACCESS_TOKEN = Read-Host "TMDB access token"
```

Replace the example database username with the username configured in your own PostgreSQL environment.

These variables apply to the current PowerShell session and are inherited by Gradle commands executed from that session.

Never commit database passwords, API tokens, or populated secret configuration files.

When sharing terminal output, remove credentials and tokens.

## 7. Run the Backend

From the repository root:

```powershell
cd backend
```

Start the application with:

```powershell
.\gradlew.bat bootRun
```

Spring Boot initializes the application and connects to PostgreSQL.

Flyway checks and applies pending database migrations during startup.

The application's HTTP port and other properties are controlled by Spring configuration.

## 8. Run Automated Tests

The backend uses JUnit, Mockito, Spring Boot Test, and PostgreSQL integration tests.

Run the normal test suite:

```powershell
.\gradlew.bat clean test
```

Run a specific test class:

```powershell
.\gradlew.bat test --tests "com.ilaridamiano.backend.integration.tmdb.service.TmdbSyncServiceTest"
```

The regular Gradle test task excludes tests tagged `external`, which are intended for live external API connectivity checks.

Database-backed tests require a running PostgreSQL instance and valid database credentials.

The test Spring profile provides test-specific application configuration, including a non-production TMDB token placeholder.

## 9. Testing in IntelliJ IDEA

Tests can also be launched from IntelliJ.

However, tests requiring database credentials may fail if the IntelliJ test run configuration does not inherit the necessary environment variables.

For consistency, running tests using the Gradle Wrapper in the configured PowerShell session is recommended.

When investigating a failure:

1. Read the first meaningful exception.
2. Check that the PostgreSQL container is running.
3. Confirm database credentials and database availability.
4. Check whether the correct Spring profile is active.
5. Run the affected test class independently.

Avoid modifying database migrations merely to bypass failing tests.

## 10. Database Migrations

Flyway migrations are stored in:

`backend/src/main/resources/db/migration/`

Migration filenames follow the convention:

`V<number>__description.sql`

The project currently includes migrations V1 through V13.

Flyway records applied migrations in its database history table.

If an applied migration needs correction, create a new migration rather than rewriting its history.

## 11. Git Workflow

The project uses the following branches:

| Branch | Purpose |
|---|---|
| `main` | Stable project milestones |
| `develop` | Integration of completed features |
| `feature/*` | Feature development |
| `fix/*` | Bug fixes |

Typical workflow:

1. Create a feature branch from `develop`.
2. Implement and test a small change.
3. Commit meaningful checkpoints.
4. Keep relevant documentation updated.
5. Run the full test suite.
6. Push the feature branch.
7. Merge reviewed and validated changes into `develop`.

Changes are promoted to `main` at stable milestones.

## 12. Development Principles

The project follows an incremental development process.

Important principles include:

- Prefer small, focused changes.
- Keep controllers lightweight.
- Put business rules in services.
- Use repositories for persistence.
- Separate external API models from internal entities.
- Write tests for success and failure scenarios.
- Use Flyway for schema changes.
- Never commit real secrets.
- Keep documentation consistent with the code.
- Avoid enabling unfinished background processes.

## 13. Current Development Status

The backend currently includes:

- Persistent media catalog
- PostgreSQL-backed searching
- TMDB movie importing and refreshing
- Synchronization retry tracking
- HTTP 401, 404, and 429 handling
- Provider-wide synchronization cooldowns
- Automated backend tests

The following are still under development:

- Authentication and authorization
- Additional external provider support
- Concurrency-safe synchronization
- Automatic background synchronization
- Personal media libraries and reviews
- Frontend application
- Production deployment

The current synchronization code is not yet suitable for unrestricted unattended execution.

## 14. Troubleshooting

### PostgreSQL connection fails

Check Docker Desktop, container status, database credentials, and the configured database name.

### Flyway migration fails

Inspect the migration error and the database schema history. Do not delete or edit applied migrations without understanding their consequences.

### Gradle cannot find Java

Verify the JDK installation, `JAVA_HOME`, and IntelliJ's configured Gradle JVM.

### Integration tests fail but unit tests pass

Confirm that the test database exists and that Gradle has access to the required environment variables.

### TMDB requests fail

Check the API token, network connectivity, TMDB response status, and any active provider-wide cooldown.

Do not expose the TMDB token when reporting issues.

---

This guide should be updated as the application gains authentication, frontend tooling, automated synchronization, and deployment infrastructure.