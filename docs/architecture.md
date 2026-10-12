# Backend Architecture

## 1. Overview

The Media Ranking Platform uses a modular Spring Boot backend built with Java 25.

The application follows a layered architecture that separates HTTP request handling, business logic, database persistence, and external API integrations.

The current backend is organized as a single Spring Boot application, rather than a collection of microservices.

This approach keeps development and deployment manageable while allowing individual features to have clearly defined responsibilities.

## 2. Technology Stack

- Java 25
- Spring Boot 4.1.1
- Spring Data JPA and Hibernate
- Spring Web and RestClient
- PostgreSQL 18
- Flyway
- Gradle
- Lombok
- JUnit, Mockito, and Spring Boot Test

## 3. Application Layers

### Controller Layer

Controllers expose REST endpoints and receive HTTP requests.

For example, `MediaController` provides media listing, searching, and TMDB movie-import endpoints.

Controllers should remain relatively lightweight. Complex business rules belong in services rather than HTTP handlers.

### Service Layer

Services implement application workflows and business rules.

Examples include:

| Service | Responsibility |
|---|---|
| `TmdbImportService` | Import and refresh movie metadata |
| `TmdbSyncService` | Select and synchronize outdated movies |
| `SyncTrackingService` | Track per-media synchronization success and failure |
| `ProviderCooldownService` | Manage provider-wide synchronization cooldowns |
| `SyncRetryPolicy` | Calculate retry delays |

Spring manages service dependencies using dependency injection.

Transactional operations use Spring's `@Transactional` support.

### Repository Layer

Repositories provide database access through Spring Data JPA.

Examples:

- `MediaRepository`
- `MediaExternalIdRepository`
- `ProviderSyncStateRepository`

Repositories contain persistence queries, while services coordinate the workflows that use them.

Some catalog searches use PostgreSQL-specific SQL features, including trigram similarity.

### Entity Layer

JPA entities represent persistent database records.

Important entities include:

- `Media`
- `MediaExternalId`
- `ProviderSyncState`

Other database tables support media titles, aliases, groups, and relationships.

Not every database structure has complete application-level functionality yet.

### Integration Layer

The integration layer communicates with external media providers.

For TMDB:

- `TmdbClient` handles HTTP communication.
- `TmdbMovieResponse` represents provider response data.
- `TmdbMovieMapper` converts external metadata into local catalog data.
- TMDB-specific exceptions distinguish permanent missing-record errors from rate limiting.

This design separates external API details from the rest of the application.

## 4. Request Flow

A typical media search follows this sequence:

1. A client sends an HTTP request to the backend.
2. `MediaController` receives and processes the request.
3. The catalog persistence layer executes the search query.
4. PostgreSQL returns matching media entries.
5. The backend returns a paginated HTTP response.

The current application supports media listing, filtering, and fuzzy searching.

## 5. TMDB Movie Import Flow

A manual TMDB import follows this sequence:

1. The client requests an import using a TMDB movie ID.
2. The controller delegates to `TmdbImportService`.
3. The service checks whether the external identity already exists locally.
4. If the movie exists, the existing record is returned.
5. Otherwise, `TmdbClient` retrieves metadata from TMDB.
6. The mapper converts provider data into application entities.
7. JPA persists the media and external identity.
8. The application returns the imported result.

A database uniqueness constraint helps protect the catalog against duplicate provider identities.

Concurrent requests still require additional safeguards.

## 6. Synchronization Architecture

The application supports manually invoked batch synchronization logic for outdated TMDB movies.

A movie is eligible when:

- Its last successful synchronization is missing or older than seven days.
- It has an appropriate TMDB external identity.
- Automatic synchronization has not been disabled for that identity.
- Its individual retry cooldown has expired.

Before processing a batch, the service checks the provider-wide cooldown state.

If TMDB is unavailable because of an active cooldown, the batch is skipped.

Otherwise, eligible movies are processed individually.

### Error Handling

The synchronization system distinguishes several outcomes:

**Successful refresh:** Update media metadata and clear individual failure tracking.

**Temporary failure:** Record a failure and calculate an exponential retry delay.

**HTTP 404:** Treat the TMDB movie as permanently unavailable and disable future automatic synchronization for that external identity.

**HTTP 401:** Stop the synchronization batch when TMDB
rejects authentication credentials. Do not record an
individual movie failure or create a rate-limit cooldown.

**HTTP 429:** Attempt to persist a provider-wide cooldown and stop the current batch.

Persistent provider-level authentication blocking and
additional concurrency controls are still being developed.

## 7. Transaction Management

Spring transactions manage persistence boundaries.

Metadata import and refresh operations use transactional service methods.

Individual synchronization tracking operations use `REQUIRES_NEW` transactions.

This allows a failed metadata refresh to be tracked separately from the transaction that attempted it.

The trade-off is that metadata changes and tracking state are not necessarily committed atomically.

Provider cooldown persistence also uses its own transaction.

## 8. Configuration

Spring application configuration provides database connectivity and external integration settings.

Sensitive configuration is supplied using environment variables, including:

- `DB_USERNAME`
- `DB_PASSWORD`
- `TMDB_ACCESS_TOKEN`

Secrets must never be committed to Git.

The application also provides an injectable `Clock` bean, allowing time-dependent logic to be tested with a deterministic clock.

## 9. Testing Architecture

The project combines several testing approaches:

| Test type | Purpose |
|---|---|
| Unit tests | Verify business rules and retry policies |
| Mockito-based service tests | Verify interactions and error handling |
| HTTP client tests | Test external API status handling with mocked responses |
| Spring Boot integration tests | Verify persistence and application behavior |
| External API checks | Manually test live TMDB connectivity |

Database integration tests use a dedicated PostgreSQL test database.

Live external API tests are excluded from the default Gradle test task.

## 10. Current Architecture Limitations

The backend is still under active development.

Before enabling automatic scheduling or deploying publicly, additional work is required:

- Authentication and authorization
- Protection of administrative import operations
- Provider authentication-error handling
- Concurrent synchronization coordination
- Stronger rate-limit recovery
- Production logging and operational monitoring
- Additional provider integrations
- User account and personal media tracking functionality

The frontend, public hosting, and complete user-facing platform are planned for later development.

## 11. Design Goals

The architectural goals are:

- Clear separation of responsibilities
- Readable and maintainable Java code
- Testable business logic
- Consistent database persistence
- Resilient external API integration
- Incremental feature development
- A foundation that can grow without premature architectural complexity

The project prioritizes correctness, maintainability, and learning before introducing unnecessary infrastructure or distributed-system complexity.