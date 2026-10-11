# Media Ranking Platform

A full-stack media discovery and tracking platform built with Java and Spring Boot.

The goal is to create a responsive, multilingual website where users can discover, rate, track, and organize movies, TV shows, anime, manga, manhwa, and manhua.

This project is both a personal learning experience and a professional portfolio project focused on backend development, REST APIs, relational database design, external API integrations, and software engineering best practices.

**Project status: In active development — backend foundation implemented.**

## Technology Stack

### Backend
- Java 25
- Spring Boot 4.1.1
- Spring Data JPA / Hibernate
- Gradle
- Lombok
- JUnit, Mockito, and Spring Boot Test

### Database
- PostgreSQL 18
- Flyway database migrations
- PostgreSQL `pg_trgm` for fuzzy text searching

### Frontend (Planned)
- React
- TypeScript
- Responsive design
- English and Italian localization

### Infrastructure
- Docker
- Docker Compose
- Git and GitHub

## Implemented Features

### Media Catalog
- Persistent media entities using PostgreSQL
- External provider ID management
- Multilingual title aliases
- Fuzzy media searching with PostgreSQL trigram similarity
- Paginated media listings
- Media type filtering
- External popularity and rating metadata

### TMDB Integration
- Import movies from TMDB
- Refresh existing movie metadata without creating duplicates
- Track successful synchronization timestamps
- Identify outdated movies
- Process synchronization in batches

### Synchronization Reliability
- Individual movie failure isolation
- Exponential retry backoff
- Persistent synchronization failure tracking
- Permanent disabling of missing TMDB records after HTTP 404
- HTTP 429 rate-limit detection
- Provider-wide cooldown tracking stored in PostgreSQL
- Batch interruption when TMDB rate limits requests
- Cooldown checks before starting synchronization batches

Automatic background scheduling is not enabled yet. Additional authentication-error handling and concurrency safeguards are still in development.

### Testing
- Unit tests for business logic and error handling
- PostgreSQL integration tests
- Mocked HTTP tests for TMDB responses
- Separate external API connectivity checks

## Current REST API

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/media` | Retrieve paginated catalog results |
| GET | `/api/media/search` | Search media titles and aliases |
| POST | `/api/media/import/tmdb/movies/{tmdbId}` | Import a movie from TMDB |

**Security note:** The manual TMDB import endpoint is intended for local development. It must be protected before public deployment.

## Planned Features

- Private user accounts and secure authentication
- Personal media libraries
- Watched, Watching, Completed, Planned, and Dropped tracking states
- User ratings from 1 to 10
- Custom lists and rankings
- User reviews
- Advanced discovery filters and sorting
- Anime, manga, manhwa, and manhua catalog integration
- English and Italian localization
- Responsive media discovery interface
- Automated provider synchronization
- Production hosting and deployment

## Project Structure

- `backend/` — Spring Boot backend application
- `frontend/` — Frontend application (planned)
- `infrastructure/` — Docker and PostgreSQL configuration
- `docs/` — Technical documentation

## Local Development

### Prerequisites

- Java 25
- Docker Desktop
- Git
- PostgreSQL database credentials
- TMDB API read access token

### Database

Start PostgreSQL using Docker Compose from the `infrastructure/` directory:

`docker compose up -d`

Ensure the development database `media_ranking` and test database `media_ranking_test` exist.

### Backend

Configure these environment variables locally:

- `DB_USERNAME`
- `DB_PASSWORD`
- `TMDB_ACCESS_TOKEN`

Never commit actual passwords or API tokens.

From the `backend/` directory, start Spring Boot:

`.\gradlew.bat bootRun`

Flyway manages database schema migrations when the application starts.

### Running Tests

From the `backend/` directory:

`.\gradlew.bat clean test`

The database-backed tests require a running PostgreSQL instance and configured database credentials.

Tests marked as external API checks are excluded from the normal Gradle test task.

## Git Workflow

- `main` — Stable project milestones
- `develop` — Integration branch
- `feature/*` — Individual feature development
- `fix/*` — Bug fixes

Features are developed and tested on separate branches before being merged into `develop`. Stable milestones are then promoted to `main`.

## Project Goals

This project demonstrates practical experience with Java backend development, including database modeling, REST API design, transactions, external services, automated testing, error handling, and maintainable application architecture.

The long-term goal is to deploy a fully functional platform that can be used by friends and other media enthusiasts.