# REST API Documentation

## 1. Overview

The Media Ranking Platform exposes REST endpoints through its Java Spring Boot backend.

The current API supports:

- Retrieving paginated media catalog results
- Filtering catalog results by media type
- Searching media titles and aliases
- Importing individual movies from TMDB

The backend is still under development. User authentication, personal media libraries, and additional catalog management endpoints are planned.

**Base path:** `/api/media`

## 2. Media Summary Response

The current controller returns `MediaSummaryResponse` objects containing:

| Field | Description |
|---|---|
| `id` | Unique local media identifier (UUID) |
| `title` | Canonical media title |
| `mediaType` | Media category |

Example conceptual response:

`{"id":"550e8400-e29b-41d4-a716-446655440000","title":"Inception","mediaType":"MOVIE"}`

The UUID above is illustrative, not an actual database record.

## 3. List Media

**GET** `/api/media`

Retrieves paginated media catalog results.

### Query Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | Integer | 0 | Zero-based page number |
| `size` | Integer | 24 | Number of records per page |
| `mediaType` | MediaType | Optional | Filter results by media category |

Supported media types:

`MOVIE`, `TV_SHOW`, `ANIME`, `MANGA`, `MANHWA`, `MANHUA`

### Examples

Get the first page:

`GET /api/media`

Get the second page with 12 records:

`GET /api/media?page=1&size=12`

Retrieve only movies:

`GET /api/media?mediaType=MOVIE`

Retrieve the first 24 manga entries:

`GET /api/media?page=0&size=24&mediaType=MANGA`

### Pagination Rules

- `page` must be greater than or equal to `0`.
- `size` must be between `1` and `100`, inclusive.
- Invalid values produce HTTP `400 Bad Request`.
- Results are sorted by `title` ascending, then `id` ascending.

### Response

**Successful status:** `200 OK`

**Java return type:** `Page<MediaSummaryResponse>`

The response contains media summary entries and Spring Data pagination information, such as the current page and total matching records.

The precise serialized pagination structure should be verified against the running API before documenting a complete JSON example.

## 4. Search Media

**GET** `/api/media/search`

Searches the media catalog by title.

### Query Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `query` | String | Yes | Search text |

### Examples

Search for Inception:

`GET /api/media/search?query=Inception`

Search for Attack on Titan:

`GET /api/media/search?query=Attack%20on%20Titan`

### Search Functionality

The underlying catalog search supports canonical titles and alternative-title matching, including PostgreSQL trigram-based fuzzy searching.

The exact matching, minimum query-length, and result-limit rules are implemented outside the controller and should be documented after reviewing `MediaService`.

### Response

**Successful status:** `200 OK`

**Java return type:** `List<MediaSummaryResponse>`

The endpoint returns a JSON array of matching media summaries rather than a paginated response.

If no records match, the expected response is an empty array, subject to the service's query-validation behavior.

If the required `query` parameter is omitted, Spring MVC returns a client error.

## 5. Import a TMDB Movie

**POST** `/api/media/import/tmdb/movies/{tmdbId}`

Imports movie metadata from TMDB into the local catalog.

### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `tmdbId` | Long | Numeric TMDB movie identifier |

### Example

`POST /api/media/import/tmdb/movies/27205`

The example ID refers to a TMDB movie identifier.

### Behavior

The controller delegates to `TmdbImportService`.

The service:

1. Checks whether the TMDB identity already exists in the database.
2. Returns the existing local media record if found.
3. Otherwise, requests the movie metadata from TMDB.
4. Maps the external response to local media data.
5. Persists the imported media and its external identity.
6. Returns the local media summary.

The external identity has a database uniqueness constraint to help prevent duplicate provider records.

### Response

**Successful status:** `200 OK`

**Java return type:** `MediaSummaryResponse`

The endpoint returns the local UUID, title, and media type of the resulting movie.

### Error Handling

The TMDB client recognizes:

- HTTP `404` from TMDB as a permanent missing-movie condition.
- HTTP `429` from TMDB as a provider rate-limit condition.

The current controller does not explicitly map these exceptions to public API error responses. Their externally visible response statuses need to be verified and standardized.

### Security Warning

This endpoint is currently intended for development.

Before public deployment, it must be protected with appropriate authentication, authorization, and request restrictions.

## 6. HTTP Status Codes

The following behavior is confirmed by the current controller implementation:

| Status | Meaning |
|---|---|
| `200 OK` | Successful listing, search, or import |
| `400 Bad Request` | Invalid pagination parameters or certain invalid request parameters |

Other errors, including external provider failures and unexpected database errors, do not yet have a documented application-wide response format.

A standardized error response design is planned.

## 7. Current Limitations

The current API does not yet provide:

- User registration or login
- User-specific watchlists or reading lists
- Personal ratings and reviews
- Media tracking status updates
- Custom user lists
- Public media-detail endpoints with expanded metadata
- Media group and relationship endpoints
- Administrative synchronization endpoints
- Fully standardized HTTP error responses
- Public API authentication and authorization

The search endpoint currently returns a list rather than a paginated result.

API versioning and a published OpenAPI specification may be added later.

## 8. Future Development

Planned API work includes:

- Secure user authentication
- User library and rating endpoints
- Rich media-detail responses
- Advanced searching and filtering
- Additional external provider integrations
- Standardized validation and error handling
- Production-safe administrative operations
- OpenAPI / Swagger documentation

This document should be updated whenever a controller's routes, parameters, response types, or validation rules change.