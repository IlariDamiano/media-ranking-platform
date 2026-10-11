
package com.ilaridamiano.backend.integration.tmdb.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.ilaridamiano.backend.integration.tmdb.client.TmdbClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.assertj.core.api.Assertions.assertThat;
import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.entity.MediaExternalId;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import com.ilaridamiano.backend.catalog.repository.MediaRepository;
import com.ilaridamiano.backend.catalog.repository.MediaExternalIdRepository;
import com.ilaridamiano.backend.integration.tmdb.dto.TmdbMovieResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import jakarta.persistence.EntityManager;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TmdbImportServiceIntegrationTest {

    @MockitoBean
    private TmdbClient tmdbClient;

    private final TmdbImportService importService;
    private final MediaRepository mediaRepository;
    private final MediaExternalIdRepository externalIdRepository;
    private final EntityManager entityManager;

    @Autowired
    TmdbImportServiceIntegrationTest(
            TmdbImportService importService,
            MediaRepository mediaRepository,
            MediaExternalIdRepository externalIdRepository,
            EntityManager entityManager
    ) {
        this.importService = importService;
        this.mediaRepository = mediaRepository;
        this.externalIdRepository = externalIdRepository;
        this.entityManager = entityManager;
    }

    @Test
    void springCanCreateImportService() {
        assertThat(importService).isNotNull();
    }


    @Test
    void importsMovieAndPersistsBothRecords() {

        TmdbMovieResponse response = new TmdbMovieResponse(
                157336L,
                "Interstellar",
                "Interstellar",
                "A science fiction adventure.",
                "2014-11-04",
                169,
                "en",
                "/poster.jpg",
                125.8,
                8.5,
                38000
        );

        when(tmdbClient.getMovie(157336L)).thenReturn(response);

        Media imported = importService.importMovie(157336L);
        entityManager.flush();
        entityManager.clear();

        assertThat(imported.getId()).isNotNull();
        assertThat(imported.getTitle()).isEqualTo("Interstellar");
        assertThat(imported.getMediaType()).isEqualTo(MediaType.MOVIE);
        assertThat(imported.getReleaseDate())
                .isEqualTo(LocalDate.of(2014, 11, 4));

        Media reloaded = mediaRepository.findById(imported.getId())
                .orElseThrow();

        assertThat(reloaded.getTitle()).isEqualTo("Interstellar");
        assertThat(reloaded.getReleaseDate())
                .isEqualTo(LocalDate.of(2014, 11, 4));

        MediaExternalId externalId = externalIdRepository
                .findByProviderAndProviderMediaTypeAndExternalId(
                        "TMDB", "MOVIE", "157336"
                )
                .orElseThrow();

        assertThat(externalId.getMedia().getId())
                .isEqualTo(imported.getId());

        assertThat(reloaded.getPopularityScore())
                .isEqualTo(125.8);

        assertThat(reloaded.getExternalRating())
                .isEqualTo(8.5);

        assertThat(reloaded.getExternalRatingCount())
                .isEqualTo(38000);

        assertThat(reloaded.getLastSyncedAt()).isNotNull();
    }


    @Test
    void importingSameMovieTwiceDoesNotCreateDuplicates() {

        TmdbMovieResponse response = new TmdbMovieResponse(
                157336L,
                "Interstellar",
                "Interstellar",
                "A science fiction adventure.",
                "2014-11-04",
                169,
                "en",
                "/poster.jpg",
                125.8,
                8.5,
                38000
        );

        when(tmdbClient.getMovie(157336L)).thenReturn(response);

        Media firstImport = importService.importMovie(157336L);
        Media secondImport = importService.importMovie(157336L);

        assertThat(secondImport.getId()).isEqualTo(firstImport.getId());
        entityManager.flush();
        entityManager.clear();

        long matchingExternalIds = externalIdRepository.findAll()
                .stream()
                .filter(externalId ->
                        externalId.getProvider().equals("TMDB")
                                && externalId.getProviderMediaType().equals("MOVIE")
                                && externalId.getExternalId().equals("157336"))
                .count();

        assertThat(matchingExternalIds).isEqualTo(1);
        verify(tmdbClient, times(1)).getMovie(157336L);
    }

    @Test
    void refreshesMovieMetadataWithoutCreatingDuplicates() {

        // Arrange: initial TMDB response
        TmdbMovieResponse initialResponse = new TmdbMovieResponse(
                157336L,
                "Interstellar",
                "Interstellar",
                "A science fiction adventure.",
                "2014-11-04",
                169,
                "en",
                "/poster.jpg",
                125.8,
                8.5,
                38000
        );

        when(tmdbClient.getMovie(157336L))
                .thenReturn(initialResponse);

        // Import the movie for the first time
        Media original = importService.importMovie(157336L);
        UUID originalId = original.getId();

        Instant originalSyncTime = original.getLastSyncedAt();

        // Arrange: TMDB now returns updated statistics
        TmdbMovieResponse updatedResponse = new TmdbMovieResponse(
                157336L,
                "Interstellar",
                "Interstellar",
                "A science fiction adventure.",
                "2014-11-04",
                169,
                "en",
                "/poster.jpg",
                200.5,
                8.7,
                40000
        );

        when(tmdbClient.getMovie(157336L))
                .thenReturn(updatedResponse);

        // Act: refresh the existing movie
        Media refreshed = importService.refreshMovie(157336L);

        entityManager.flush();
        entityManager.clear();

        // Reload from PostgreSQL
        Media reloaded = mediaRepository.findById(originalId)
                .orElseThrow();

        // Verify that the UUID stays the same
        assertThat(refreshed.getId()).isEqualTo(originalId);
        assertThat(reloaded.getId()).isEqualTo(originalId);

        // Verify that the statistics were updated
        assertThat(reloaded.getPopularityScore())
                .isEqualTo(200.5);

        assertThat(reloaded.getExternalRating())
                .isEqualTo(8.7);

        assertThat(reloaded.getExternalRatingCount())
                .isEqualTo(40000);

        // Verify that the title was preserved
        assertThat(reloaded.getTitle()).isEqualTo("Interstellar");

        // Verify that no duplicate media record was created
        assertThat(mediaRepository.count()).isEqualTo(1);

        // One TMDB request for import, another for refresh
        verify(tmdbClient, times(2)).getMovie(157336L);

        assertThat(originalSyncTime).isNotNull();

        assertThat(reloaded.getLastSyncedAt())
                .isNotNull();

        assertThat(reloaded.getLastSyncedAt())
                .isAfterOrEqualTo(originalSyncTime);

    }

    @Test
    void rejectsRefreshWhenMovieDoesNotExist() {

        Long unknownTmdbId = 999999999L;

        assertThatThrownBy(
                () -> importService.refreshMovie(unknownTmdbId)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "Movie not imported yet: TMDB ID " + unknownTmdbId
                );

        verify(tmdbClient, times(0))
                .getMovie(unknownTmdbId);
    }


}
