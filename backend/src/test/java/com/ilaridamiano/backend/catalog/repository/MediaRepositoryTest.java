package com.ilaridamiano.backend.catalog.repository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import com.ilaridamiano.backend.catalog.entity.MediaAlias;
import com.ilaridamiano.backend.catalog.entity.MediaExternalId;
import com.ilaridamiano.backend.catalog.repository.MediaExternalIdRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MediaRepositoryTest {


    private final MediaRepository mediaRepository;
    private final EntityManager entityManager;
    private final MediaAliasRepository mediaAliasRepository;
    private final MediaExternalIdRepository externalIdRepository;

    @Autowired
    MediaRepositoryTest(
            MediaRepository mediaRepository,
            EntityManager entityManager,
            MediaAliasRepository mediaAliasRepository,
            MediaExternalIdRepository externalIdRepository
    ) {
        this.mediaRepository = mediaRepository;
        this.entityManager = entityManager;
        this.mediaAliasRepository = mediaAliasRepository;
        this.externalIdRepository = externalIdRepository;
    }

    @Test
    @Transactional
    void canSaveMedia() {

        // Arrange
        Media movie = new Media("Interstellar", MediaType.MOVIE);

        // Act: save to PostgreSQL
        Media savedMovie = mediaRepository.saveAndFlush(movie);

        // Remember the generated ID
        UUID movieId = savedMovie.getId();

        // Clear Hibernate's in-memory entities
        entityManager.clear();

        // Retrieve the movie from PostgreSQL
        Media retrievedMovie = mediaRepository.findById(movieId)
                .orElseThrow();

        // Assert
        assertThat(movieId).isNotNull();
        assertThat(movieId.version()).isEqualTo(7);
        assertThat(retrievedMovie.getTitle()).isEqualTo("Interstellar");
        assertThat(retrievedMovie.getMediaType()).isEqualTo(MediaType.MOVIE);
    }

    @Test
    @Transactional
    void findsMoviesEvenWithTypos() {

        Media movie = new Media("Interstellar", MediaType.MOVIE);
        mediaRepository.saveAndFlush(movie);

        List<Media> results =
                mediaRepository.searchByRelevance("intresstllar");

        assertThat(results)
                .extracting(Media::getTitle)
                .contains("Interstellar");
    }

    @Test
    @Transactional
    void ranksExactMatchesBeforeFuzzyMatches() {

        mediaRepository.saveAndFlush(
                new Media("Interstellar", MediaType.MOVIE)
        );

        mediaRepository.saveAndFlush(
                new Media("Interstellaria", MediaType.MOVIE)
        );

        List<Media> results =
                mediaRepository.searchByRelevance("interstellar");

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().getTitle())
                .isEqualTo("Interstellar");
    }


    @Test
    @Transactional
    void searchesAliasesWithoutDuplicateMediaResults() {

        Media anime = mediaRepository.saveAndFlush(
                new Media("Attack on Titan", MediaType.ANIME)
        );

        mediaAliasRepository.saveAndFlush(
                new MediaAlias(
                        anime,
                        "L'attacco dei giganti",
                        "it",
                        "ALTERNATIVE"
                )
        );

        mediaAliasRepository.saveAndFlush(
                new MediaAlias(
                        anime,
                        "Attacco dei giganti",
                        "it",
                        "ALTERNATIVE"
                )
        );

        List<Media> exactResults =
                mediaRepository.searchByRelevance("L'attacco dei giganti");

        assertThat(exactResults)
                .extracting(Media::getId)
                .contains(anime.getId());

        List<Media> typoResults =
                mediaRepository.searchByRelevance("attaco dei giganti");

        assertThat(typoResults)
                .extracting(Media::getId)
                .contains(anime.getId());

        long matchingEntries = typoResults.stream()
                .filter(media -> media.getId().equals(anime.getId()))
                .count();

        assertThat(matchingEntries).isEqualTo(1);
    }


    @Test
    @Transactional
    void savesAndRetrievesDiscoveryMetadata() {

        // Arrange
        Media movie = new Media("Inception", MediaType.MOVIE);

        movie.setPopularityScore(125.8);
        movie.setExternalRating(8.5);
        movie.setExternalRatingCount(38000);

        // Save to PostgreSQL
        Media saved = mediaRepository.saveAndFlush(movie);
        UUID movieId = saved.getId();

        // Clear Hibernate's persistence context
        entityManager.clear();

        // Retrieve from PostgreSQL
        Media retrieved = mediaRepository.findById(movieId)
                .orElseThrow();

        // Assert
        assertThat(retrieved.getPopularityScore())
                .isEqualTo(125.8);

        assertThat(retrieved.getExternalRating())
                .isEqualTo(8.5);

        assertThat(retrieved.getExternalRatingCount())
                .isEqualTo(38000);
    }

    @Test
    @Transactional
    void findsOnlyMoviesThatNeedSynchronization() {

        Instant now = Instant.now();
        Instant cutoff = now.minus(7, ChronoUnit.DAYS);

        Media oldMovie = new Media("Old Movie", MediaType.MOVIE);
        oldMovie.setLastSyncedAt(now.minus(10, ChronoUnit.DAYS));

        Media recentMovie = new Media("Recent Movie", MediaType.MOVIE);
        recentMovie.setLastSyncedAt(now.minus(1, ChronoUnit.DAYS));

        Media neverSyncedMovie = new Media(
                "Never Synced Movie", MediaType.MOVIE
        );

        Media oldAnime = new Media("Old Anime", MediaType.ANIME);
        oldAnime.setLastSyncedAt(now.minus(10, ChronoUnit.DAYS));

        mediaRepository.saveAndFlush(oldMovie);
        mediaRepository.saveAndFlush(recentMovie);
        mediaRepository.saveAndFlush(neverSyncedMovie);
        mediaRepository.saveAndFlush(oldAnime);

        externalIdRepository.saveAndFlush(
                new MediaExternalId(
                        oldMovie, "TMDB", "MOVIE", "1001"
                )
        );

        externalIdRepository.saveAndFlush(
                new MediaExternalId(
                        recentMovie, "TMDB", "MOVIE", "1002"
                )
        );

        externalIdRepository.saveAndFlush(
                new MediaExternalId(
                        neverSyncedMovie, "TMDB", "MOVIE", "1003"
                )
        );

        Media manualMovie = new Media(
                "Manual Movie", MediaType.MOVIE
        );

        manualMovie.setLastSyncedAt(
                now.minus(20, ChronoUnit.DAYS)
        );

        mediaRepository.saveAndFlush(manualMovie);

        entityManager.clear();

        Page<Media> results = mediaRepository.findMediaNeedingSync(
                MediaType.MOVIE,
                cutoff,
                Instant.now(),
                PageRequest.of(0, 20)
        );


        assertThat(results.getContent())
                .extracting(Media::getTitle)
                .contains("Old Movie", "Never Synced Movie")
                .doesNotContain(
                        "Recent Movie",
                        "Old Anime",
                        "Manual Movie"
                );

    }

    @Test
    @Transactional
    void findsTmdbExternalIdByMediaId() {

        // Arrange: create and save a movie
        Media movie = mediaRepository.saveAndFlush(
                new Media("Interstellar", MediaType.MOVIE)
        );

        // Associate the movie with its TMDB identifier
        externalIdRepository.saveAndFlush(
                new MediaExternalId(
                        movie,
                        "TMDB",
                        "MOVIE",
                        "157336"
                )
        );

        UUID movieId = movie.getId();

        // Clear Hibernate's persistence context
        entityManager.clear();

        // Act: find the TMDB identifier
        MediaExternalId externalId = externalIdRepository
                .findByMediaIdAndProviderAndProviderMediaType(
                        movieId,
                        "TMDB",
                        "MOVIE"
                )
                .orElseThrow();

        // Assert
        assertThat(externalId.getExternalId())
                .isEqualTo("157336");

        assertThat(externalId.getMedia().getId())
                .isEqualTo(movieId);
    }

    @Test
    @Transactional
    void findsIncompleteAndMistypedTitles() {

        Media interstellar = mediaRepository.saveAndFlush(
                new Media("Interstellar", MediaType.MOVIE)
        );

        List<String> queries = List.of(
                "interstellar",
                "intrestellar",
                "itestellar",
                "intres",
                "itestell"
        );

        for (String query : queries) {

            List<Media> results =
                    mediaRepository.searchByRelevance(query);

            assertThat(results)
                    .as("Search '%s' should find Interstellar", query)
                    .extracting(Media::getId)
                    .contains(interstellar.getId());
        }
    }

}