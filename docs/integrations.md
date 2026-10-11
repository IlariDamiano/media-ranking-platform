# External API Integrations

This document describes how the Media Ranking Platform integrates with external media catalog providers.

## Integration Overview

The platform uses external providers to retrieve media metadata and populate its local PostgreSQL catalog.

The goal is to maintain a searchable media database while storing application-specific information, such as user ratings and personal lists, independently of external providers.

| Provider | Purpose | Status |
|---|---|---|
| TMDB | Movies and TV metadata | Movie integration implemented |
| AniList | Anime and manga metadata | Planned |

TV synchronization and additional provider integrations are not yet implemented.

## 1. TMDB Integration

The TMDB integration is located in:

`backend/src/main/java/com/ilaridamiano/backend/integration/tmdb/`

### Main Components

| Component | Responsibility |
|---|---|
| `TmdbClient` | Communicates with the TMDB REST API |
| `TmdbMovieResponse` | Represents movie metadata returned by TMDB |
| `TmdbMovieMapper` | Converts TMDB data into local media entities |
| `TmdbImportService` | Imports movies and refreshes existing metadata |
| `TmdbSyncService` | Selects and processes outdated movies |
| `SyncTrackingService` | Records individual synchronization results |
| `SyncRetryPolicy` | Calculates retry delays |

Provider-wide cooldown behavior is implemented separately in `integration.sync.service.ProviderCooldownService`.

## 2. Movie Import Workflow

The backend supports importing a movie using its TMDB ID.

The basic workflow is:

1. Receive the requested TMDB movie ID.
2. Check whether that external ID already exists in the local catalog.
3. If already imported, return the existing media record.
4. Otherwise, retrieve movie metadata from TMDB.
5. Map the response to a local `Media` entity.
6. Persist the media and its external ID association.

The external ID association prevents repeated imports of the same TMDB movie from creating duplicate catalog records.

**Current limitation:** The manual import endpoint is intended for development and must be secured before public deployment.

## 3. Movie Synchronization

The synchronization service identifies movies whose metadata needs refreshing.

Currently, a movie is considered outdated when its last successful synchronization is missing or more than seven days old.

Eligible movies are processed in configurable batches of 1–100 records.

The process includes:

1. Check whether the TMDB provider has an active cooldown.
2. Select outdated movies that are eligible for synchronization.
3. Resolve each movie's TMDB external ID.
4. Retrieve updated metadata.
5. Update the existing media record.
6. Record the result of the synchronization attempt.

Successful refreshes update the existing media rather than creating duplicate records.

The batch processor counts successful refreshes and isolates ordinary movie-specific failures so other movies can continue processing.

Automatic background scheduling is **not yet enabled**.

## 4. Synchronization Failure Tracking

The `media_external_ids` table stores provider-specific synchronization state.

Relevant fields include:

| Field | Description |
|---|---|
| `last_sync_attempt_at` | Most recent tracked synchronization attempt |
| `next_sync_attempt_at` | Earliest allowed retry |
| `sync_failure_count` | Consecutive synchronization failures |
| `sync_disabled` | Whether automatic synchronization is disabled for the external ID |

Successful synchronization clears the failure count and retry cooldown.

Temporary failures increase the failure count and calculate a new retry timestamp.

### Exponential Backoff

The current retry policy is:

| Consecutive failures | Retry delay |
|---|---|
| 1 | 15 minutes |
| 2 | 30 minutes |
| 3 | 60 minutes |
| 4 | 120 minutes |
| 5 | 240 minutes |
| 6 or more | 360 minutes |

The delay is capped at six hours.

Synchronization queries exclude external IDs whose cooldown is still active.

## 5. HTTP Error Handling

TMDB API responses are classified according to their HTTP status.

| HTTP status | Current behavior |
|---|---|
| 200 | Process the returned metadata |
| 404 | Throw `TmdbPermanentException` |
| 429 | Throw `TmdbRateLimitException` |
| Other 4xx/5xx | Use Spring's default HTTP error handling |

### HTTP 404 — Missing Media

A confirmed missing movie produces `TmdbPermanentException`.

During batch synchronization:

- The failure is recorded.
- `sync_disabled` becomes `true`.
- Future synchronization queries exclude the disabled external ID.

The local media record is not deleted.

### HTTP 429 — Rate Limiting

TMDB rate limiting affects the provider rather than a particular movie.

When HTTP 429 occurs during a synchronization batch:

1. The client throws `TmdbRateLimitException`.
2. The batch processor attempts to record a provider-wide cooldown.
3. The current batch stops.
4. Future batches check provider availability before starting.

The rate-limited movie is not automatically classified as permanently missing.

## 6. Provider-Wide Cooldowns

The shared `provider_sync_state` table tracks cooldowns for external providers.

Its fields are:

- `provider` — Unique provider identifier.
- `cooldown_until` — Time before which requests should be deferred.
- `updated_at` — Last update of the provider state.

`ProviderCooldownService` manages this information.

The current fallback cooldown after HTTP 429 is **30 minutes**.

If an existing cooldown is longer than a newly proposed cooldown, sequential updates preserve the longer value.

Before a batch begins, the service checks whether the provider has an active cooldown.

If the cooldown is active, the batch is skipped.

## 7. Transactions and Persistence

The integration uses Spring Data JPA and PostgreSQL.

Metadata refreshes and synchronization tracking are managed through Spring transactions.

Failure-tracking operations use `REQUIRES_NEW` to allow tracking changes to commit independently of a failed metadata refresh transaction.

This improves resilience but also means metadata and tracking changes are not always committed atomically.

The application logs tracking failures for investigation.

## 8. Testing Strategy

The integration uses several types of automated tests:

- Unit tests for retry calculations and batch processing.
- Mocked HTTP tests for TMDB 404 and 429 responses.
- PostgreSQL integration tests for imports, synchronization state, and cooldowns.
- Optional live TMDB connectivity tests, excluded from the default Gradle test task.

The normal test suite does not require contacting TMDB, but database integration tests require a running PostgreSQL instance.

## 9. Current Limitations and Future Improvements

Before enabling unattended synchronization or deploying publicly, the platform still needs:

- Explicit handling of provider authentication failures, including HTTP 401.
- Support for provider-supplied `Retry-After` headers.
- Concurrency protection against overlapping synchronization jobs and concurrent cooldown updates.
- Safer recovery when a provider cooldown cannot be persisted.
- Additional safeguards for concurrent imports and synchronization.
- Production-ready authorization for import