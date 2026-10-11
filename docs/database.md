# Database Architecture

This document describes the PostgreSQL database architecture of the Media Ranking Platform, including its schema, relationships, indexes, synchronization state, and Flyway migrations.

## 1. Technology Stack

| Technology | Purpose |
|---|---|
| PostgreSQL 18 | Relational database |
| Spring Data JPA | Repository and persistence abstraction |
| Hibernate | Object-relational mapping |
| Flyway | Version-controlled database migrations |
| pg_trgm | Fuzzy and similarity-based text searching |
| UUIDv7 | Time-ordered identifiers |

The application uses a dedicated development database, `media_ranking`, and a separate test database, `media_ranking_test`.

The database schema is managed through Flyway rather than automatic Hibernate schema generation.

Hibernate is configured with `ddl-auto=validate`.

## 2. Database Design

The database is organized around a shared media catalog.

Instead of maintaining completely separate tables for movies, anime, and manga, the platform uses a common `media` table with a `media_type` discriminator.

This enables shared discovery, searching, and future cross-media functionality.

Supported media categories:

- `MOVIE`
- `TV_SHOW`
- `ANIME`
- `MANGA`
- `MANHWA`
- `MANHUA`

Database support for a media category does not necessarily mean its complete import pipeline or frontend functionality has been implemented.

## 3. Core Tables

### 3.1 `media`

The primary table representing individual media entries.

**Primary key:** `id UUID`

Key columns include:

| Column | PostgreSQL type | Description |
|---|---|---|
| `id` | UUID | Unique media identifier |
| `title` | VARCHAR(300) | Canonical display title |
| `media_type` | VARCHAR(20) | Media category |
| `description` | TEXT | Synopsis or description |
| `release_date` | DATE | Original release date |
| `end_date` | DATE | End date, when applicable |
| `release_year` | SMALLINT | Release year |
| `release_status` | VARCHAR(30) | Publication or release state |
| `format` | VARCHAR(30) | Provider-independent format metadata |
| `original_language` | VARCHAR(15) | Original language |
| `origin_country` | CHAR(2) | Country code |
| `is_adult` | BOOLEAN | Adult-content indicator |
| `created_at` | TIMESTAMPTZ | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | Update timestamp |
| `popularity_score` | DOUBLE PRECISION | External popularity score |
| `external_rating` | DOUBLE PRECISION | External average rating |
| `external_rating_count` | INTEGER | Number of external ratings |
| `last_synced_at` | TIMESTAMPTZ | Last successful metadata synchronization |

The table includes constraints that prevent blank titles, restrict media types and release statuses, validate release years, and prevent end dates preceding release dates when both are present.

UUIDv7 provides identifiers with useful time-ordering characteristics.

### 3.2 `media_external_ids`

Associates local media entries with identifiers from external providers.

Key columns:

| Column | Description |
|---|---|
| `id` | UUID primary key |
| `media_id` | Foreign key to `media.id` |
| `provider` | External provider, such as TMDB |
| `provider_media_type` | Provider-specific media category |
| `external_id` | Media identifier assigned by the provider |
| `last_sync_attempt_at` | Last recorded synchronization attempt |
| `next_sync_attempt_at` | Earliest eligible retry |
| `sync_failure_count` | Consecutive failures |
| `sync_disabled` | Whether automatic synchronization is disabled |

A unique constraint on `(provider, provider_media_type, external_id)` prevents the same provider identity from being associated with multiple catalog entries.

The foreign key uses `ON DELETE CASCADE`.

This table currently supports TMDB movie imports and synchronization tracking.

### 3.3 `media_aliases`

Stores alternative titles associated with media entries.

Fields include a UUID primary key, media foreign key, title, optional language code, and alias type.

The table supports multilingual and alternative-title searching.

A PostgreSQL trigram index enables fuzzy matching against alias titles.

**Implementation status:** Used by the current media search functionality.

### 3.4 `media_titles`

Introduced by Flyway V3 to store structured multilingual titles.

Its principal columns are:

- `id`
- `media_id`
- `title`
- `language_code`
- `title_type`

Supported title types include `PRIMARY`, `ENGLISH`, `ITALIAN`, `ORIGINAL`, `ROMANIZED`, and `ALTERNATIVE`.

This table has foreign-key and nonblank-title constraints, plus case-insensitive and trigram search indexes.

**Implementation note:** Both `media_titles` and `media_aliases` currently exist in the schema. The implemented fuzzy search uses `media_aliases`. Their long-term responsibilities should be reviewed before building full multilingual title-management features.

## 4. Media Grouping and Relationships

Flyway V3 introduced tables for representing franchises, collections, and relationships between media entries.

These tables provide database foundations for future catalog features; their complete application-level functionality is not yet implemented.

### 4.1 `media_groups`

Represents a collection of related media.

Examples could include a film franchise, anime series collection, or shared fictional universe.

Key columns include:

- `id`
- `name`
- `description`
- `group_type`
- `primary_media_type`
- `created_at`
- `updated_at`

Allowed group types are `SERIES`, `FRANCHISE`, and `COLLECTION`.

### 4.2 `media_group_members`

Connects media entries to groups.

Each membership includes:

- `group_id`
- `media_id`
- `section`
- `display_order`

The `(group_id, media_id)` combination must be unique.

Sections include `MAIN_STORY`, `MOVIES`, `EXTRAS`, `SPIN_OFFS`, and `RELATED`.

A nonnegative display order supports ordered presentation within a group.

### 4.3 `media_relations`

Represents relationships between individual media entries.

Its important fields are:

- `source_media_id`
- `target_media_id`
- `relation_type`

Supported relations include sequels, prequels, adaptations, sources, side stories, spin-offs, alternatives, summaries, character relationships, and other relations.

Database constraints prevent self-relations and duplicate relationships with the same source, target, and relation type.

### 4.4 `media_group_titles`

Stores multilingual and alternative names for media groups.

It includes a foreign key to `media_groups`, title content, optional language code, and a constrained title type.

The table also has case-insensitive and trigram search indexes.

## 5. Provider-Wide Synchronization

### `provider_sync_state`

Introduced in Flyway V11 to manage synchronization restrictions affecting an entire external provider.

| Column | Type | Description |
|---|---|---|
| `provider` | VARCHAR(50) | Primary key |
| `cooldown_until` | TIMESTAMPTZ | Provider-wide cooldown expiration |
| `updated_at` | TIMESTAMPTZ | Most recent state update |

Unlike `media_external_ids`, which tracks individual media records, this table tracks provider-level restrictions.

For example, an HTTP 429 response from TMDB triggers an attempt to save a provider-wide cooldown.

The current fallback cooldown is 30 minutes.

`ProviderCooldownService` uses this table to check whether synchronization should proceed.

Concurrency-safe coordination between multiple synchronization jobs is still under development.

## 6. Database Relationships

The principal relationships are:

| Parent | Child | Relationship |
|---|---|---|
| `media` | `media_external_ids` | One-to-many |
| `media` | `media_aliases` | One-to-many |
| `media` | `media_titles` | One-to-many |
| `media_groups` | `media_group_members` | One-to-many |
| `media` | `media_group_members` | One-to-many |
| `media_groups` | `media_group_titles` | One-to-many |
| `media` | `media_relations` | Source and target references |

The group-membership table provides a many-to-many-style association between media and groups.

Media relations are directed, meaning that the source and target have distinct roles.

The V3 foreign keys for these group and relation tables use cascading deletion.

The provider synchronization table is independent of any individual media record and uses its provider name as the primary key.

## 7. Search and Indexing

The platform uses PostgreSQL's `pg_trgm` extension to support similarity-based searching.

The implemented search includes:

- Case-insensitive canonical-title matching
- Partial-title matching
- Alternative-title matching
- Trigram-based fuzzy matching
- Relevance-oriented result ordering

Indexes introduced across the migrations include:

- `idx_media_type`
- `idx_media_release_date`
- `idx_media_titles_lower_title`
- `idx_media_titles_trgm`
- `idx_media_group_titles_lower_title`
- `idx_media_group_titles_trgm`
- `idx_media_title_trgm`
- `idx_media_aliases_title_trgm`

There are also indexes supporting external media associations, group membership, and media relationships.

An index speeds up appropriate queries but comes with additional storage and write-maintenance costs. Index effectiveness should be evaluated as the catalog grows.

## 8. Flyway Migration History

Database schema changes are managed through numbered SQL migrations located at:

`backend/src/main/resources/db/migration/`

| Version | Migration | Purpose |
|---|---|---|
| V1 | `create_media_table` | Create the core media table, UUID primary key, timestamps, and validation constraints |
| V2 | `expand_media_catalog` | Support six media categories, add shared metadata, constraints, and catalog indexes |
| V3 | `create_media_catalog_relations` | Create title tables, external IDs, groups, memberships, relationships, and search-supporting structures |
| V4 | `enable_pg_trgm` | Ensure PostgreSQL trigram support is enabled |
| V5 | `create_media_aliases` | Add the media aliases table |
| V6 | `add_media_search_indexes` | Add trigram indexes for canonical titles and aliases |
| V7 | `add_media_discovery_scores` | Add external popularity and rating metadata |
| V8 | `add_media_sync_timestamp` | Add the last-successful-sync timestamp |
| V9 | `add_external_id_sync_tracking` | Add attempt timestamps, retry timestamps, and failure counts |
| V10 | `add_external_id_sync_disabled` | Add permanent synchronization-disable state |
| V11 | `create_provider_sync_state` | Add provider-wide synchronization cooldown tracking |

**Migration note:** V3 already enables `pg_trgm` and creates trigram indexes for its title tables. V4 separately ensures that the extension is enabled. Applied migrations must not be edited casually because Flyway validates their stored checksums.

New schema changes should be introduced through additional migrations.

## 9. Data Integrity and Persistence

The database uses several integrity mechanisms:

**Primary keys:** Identify records uniquely.

**Foreign keys:** Maintain relationships between media entries, external IDs, titles, and groups.

**Unique constraints:** Prevent duplicate external identities and specified duplicate relationships.

**Check constraints:** Validate media categories, title content, release metadata, ordering values, and synchronization failure counts.

**Transactions:** Ensure related persistence operations can be committed or rolled back consistently.

Synchronization tracking uses separate `REQUIRES_NEW` transactions so a failed metadata refresh does not automatically roll back its associated failure record.

This design improves resilience but requires care because metadata and tracking changes are not fully atomic.

## 10. Testing

Database-related tests run against `media_ranking_test`, separate from the development database.

Integration tests verify:

- Media persistence
- External identity persistence
- Multilingual alias searching
- Pagination and media filtering
- Outdated-media selection
- Retry cooldown eligibility
- Permanent synchronization disabling
- Provider-wide cooldown persistence and availability
- TMDB import and refresh behavior

Flyway migrations are applied to the test database when its Spring application context initializes.

Database integration tests require a running PostgreSQL instance and configured environment variables.

## 11. Planned Improvements

The following remain future work:

- User accounts and ownership relationships
- Personal media libraries and tracking statuses
- User ratings and reviews
- Custom user lists and rankings
- Genres, tags, and advanced discovery filtering
- Complete Java services and API endpoints for media groups and relationships
- Provider-specific synchronization improvements
- Concurrency-safe synchronization coordination
- Additional catalog-provider integrations
- Production database deployment and operational monitoring

The schema will evolve through new Flyway migrations as these features are implemented.