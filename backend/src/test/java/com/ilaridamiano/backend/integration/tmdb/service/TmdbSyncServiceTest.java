
package com.ilaridamiano.backend.integration.tmdb.service;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.entity.MediaExternalId;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import com.ilaridamiano.backend.catalog.repository.MediaExternalIdRepository;
import com.ilaridamiano.backend.catalog.repository.MediaRepository;
import com.ilaridamiano.backend.integration.tmdb.exception.TmdbPermanentException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import com.ilaridamiano.backend.integration.tmdb.exception.TmdbRateLimitException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.ilaridamiano.backend.integration.sync.service.ProviderCooldownService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.ilaridamiano.backend.integration.tmdb.exception.TmdbAuthenticationException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TmdbSyncServiceTest {

    @Test
    void refreshesSelectedMoviesAndReturnsSuccessCount() {

        MediaRepository mediaRepository = mock(MediaRepository.class);
        MediaExternalIdRepository externalIdRepository =
                mock(MediaExternalIdRepository.class);
        TmdbImportService importService = mock(TmdbImportService.class);
        SyncTrackingService trackingService = mock(SyncTrackingService.class);
        ProviderCooldownService cooldownService =
                mock(ProviderCooldownService.class);

        TmdbSyncService syncService = new TmdbSyncService(
                mediaRepository,
                externalIdRepository,
                importService,
                trackingService,
                cooldownService
        );

        Media movie = new Media("Interstellar", MediaType.MOVIE);

        MediaExternalId externalId = new MediaExternalId(
                movie,
                "TMDB",
                "MOVIE",
                "157336"
        );
        ReflectionTestUtils.setField(movie, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(externalId, "id", UUID.randomUUID());
        when(mediaRepository.findMediaNeedingSync(
                eq(MediaType.MOVIE),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(movie)));

        when(externalIdRepository
                .findByMediaIdAndProviderAndProviderMediaType(
                        eq(movie.getId()),
                        eq("TMDB"),
                        eq("MOVIE")
                ))
                .thenReturn(Optional.of(externalId));

        when(cooldownService.isProviderAvailable("TMDB"))
                .thenReturn(true);

        int refreshedCount = syncService.syncOutdatedMovies(10);

        assertThat(refreshedCount).isEqualTo(1);

        verify(importService, times(1))
                .refreshMovie(157336L);
        verify(trackingService).recordSuccess(externalId.getId());
        verify(trackingService, never()).recordFailure(any());
    }

    @Test
    void continuesSynchronizingWhenOneMovieFails() {

        MediaRepository mediaRepository = mock(MediaRepository.class);
        MediaExternalIdRepository externalIdRepository =
                mock(MediaExternalIdRepository.class);
        TmdbImportService importService = mock(TmdbImportService.class);
        SyncTrackingService trackingService = mock(SyncTrackingService.class);
        ProviderCooldownService cooldownService =
                mock(ProviderCooldownService.class);
        TmdbSyncService syncService = new TmdbSyncService(
                mediaRepository,
                externalIdRepository,
                importService,
                trackingService,
                cooldownService
        );

        Media firstMovie = new Media("Interstellar", MediaType.MOVIE);
        Media secondMovie = new Media("Inception", MediaType.MOVIE);

        ReflectionTestUtils.setField(firstMovie, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(secondMovie, "id", UUID.randomUUID());

        MediaExternalId firstExternalId = new MediaExternalId(
                firstMovie, "TMDB", "MOVIE", "157336"
        );

        MediaExternalId secondExternalId = new MediaExternalId(
                secondMovie, "TMDB", "MOVIE", "27205"
        );

        ReflectionTestUtils.setField(firstExternalId, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(secondExternalId, "id", UUID.randomUUID());

        // Simulate a batch containing two movies
        when(mediaRepository.findMediaNeedingSync(
                eq(MediaType.MOVIE),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(firstMovie, secondMovie)));

        // The entities are not persisted, so their IDs are null.
        // Return a different external ID on each lookup.
        when(externalIdRepository.findByMediaIdAndProviderAndProviderMediaType(
                eq(firstMovie.getId()), eq("TMDB"), eq("MOVIE")
        )).thenReturn(Optional.of(firstExternalId));

        when(externalIdRepository.findByMediaIdAndProviderAndProviderMediaType(
                eq(secondMovie.getId()), eq("TMDB"), eq("MOVIE")
        )).thenReturn(Optional.of(secondExternalId));

        // Simulate TMDB failing for Interstellar
        doThrow(new RuntimeException("TMDB temporarily unavailable"))
                .when(importService)
                .refreshMovie(157336L);

        when(cooldownService.isProviderAvailable("TMDB"))
                .thenReturn(true);

        int refreshedCount = syncService.syncOutdatedMovies(2);

        assertThat(refreshedCount).isEqualTo(1);

        verify(importService).refreshMovie(157336L);
        verify(importService).refreshMovie(27205L);
        verify(trackingService).recordFailure(firstExternalId.getId());
        verify(trackingService).recordSuccess(secondExternalId.getId());
    }

    @Test
    void recordsPermanentFailureWhenTmdbMovieIsNotFound() {

        MediaRepository mediaRepository = mock(MediaRepository.class);
        MediaExternalIdRepository externalIdRepository =
                mock(MediaExternalIdRepository.class);
        TmdbImportService importService = mock(TmdbImportService.class);
        SyncTrackingService trackingService = mock(SyncTrackingService.class);
        ProviderCooldownService cooldownService =
                mock(ProviderCooldownService.class);

        TmdbSyncService syncService = new TmdbSyncService(
                mediaRepository,
                externalIdRepository,
                importService,
                trackingService,
                cooldownService
        );

        Media movie = new Media("Missing Movie", MediaType.MOVIE);

        ReflectionTestUtils.setField(
                movie, "id", UUID.randomUUID()
        );

        MediaExternalId externalId = new MediaExternalId(
                movie, "TMDB", "MOVIE", "999999"
        );

        ReflectionTestUtils.setField(
                externalId, "id", UUID.randomUUID()
        );

        when(mediaRepository.findMediaNeedingSync(
                eq(MediaType.MOVIE),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(movie)));

        when(externalIdRepository.findByMediaIdAndProviderAndProviderMediaType(
                eq(movie.getId()),
                eq("TMDB"),
                eq("MOVIE")
        )).thenReturn(Optional.of(externalId));

        doThrow(new TmdbPermanentException("TMDB movie not found"))
                .when(importService)
                .refreshMovie(999999L);

        when(cooldownService.isProviderAvailable("TMDB"))
                .thenReturn(true);

        int refreshedCount = syncService.syncOutdatedMovies(10);

        assertThat(refreshedCount).isZero();

        verify(trackingService).recordPermanentFailure(externalId.getId());

        verify(trackingService, never()).recordFailure(any());
        verify(trackingService, never()).recordSuccess(any());
    }

    @Test
    void stopsBatchWhenTmdbRateLimitIsReached() {

        MediaRepository mediaRepository = mock(MediaRepository.class);
        MediaExternalIdRepository externalIdRepository =
                mock(MediaExternalIdRepository.class);
        TmdbImportService importService = mock(TmdbImportService.class);
        SyncTrackingService trackingService = mock(SyncTrackingService.class);
        ProviderCooldownService cooldownService =
                mock(ProviderCooldownService.class);

        TmdbSyncService syncService = new TmdbSyncService(
                mediaRepository,
                externalIdRepository,
                importService,
                trackingService,
                cooldownService
        );

        Media firstMovie = new Media("Interstellar", MediaType.MOVIE);
        Media secondMovie = new Media("Inception", MediaType.MOVIE);
        Media thirdMovie = new Media("The Dark Knight", MediaType.MOVIE);

        ReflectionTestUtils.setField(firstMovie, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(secondMovie, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(thirdMovie, "id", UUID.randomUUID());

        MediaExternalId firstExternalId = new MediaExternalId(
                firstMovie, "TMDB", "MOVIE", "157336"
        );
        MediaExternalId secondExternalId = new MediaExternalId(
                secondMovie, "TMDB", "MOVIE", "27205"
        );
        MediaExternalId thirdExternalId = new MediaExternalId(
                thirdMovie, "TMDB", "MOVIE", "155"
        );

        ReflectionTestUtils.setField(firstExternalId, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(secondExternalId, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(thirdExternalId, "id", UUID.randomUUID());

        when(mediaRepository.findMediaNeedingSync(
                eq(MediaType.MOVIE),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(List.of(firstMovie, secondMovie, thirdMovie))
        );

        when(externalIdRepository.findByMediaIdAndProviderAndProviderMediaType(
                eq(firstMovie.getId()), eq("TMDB"), eq("MOVIE")
        )).thenReturn(Optional.of(firstExternalId));

        when(externalIdRepository.findByMediaIdAndProviderAndProviderMediaType(
                eq(secondMovie.getId()), eq("TMDB"), eq("MOVIE")
        )).thenReturn(Optional.of(secondExternalId));

        doThrow(new TmdbRateLimitException("TMDB rate limit exceeded"))
                .when(importService)
                .refreshMovie(27205L);

        when(cooldownService.isProviderAvailable("TMDB"))
                .thenReturn(true);

        int refreshedCount = syncService.syncOutdatedMovies(3);

        assertThat(refreshedCount).isEqualTo(1);

        verify(importService).refreshMovie(157336L);
        verify(importService).refreshMovie(27205L);
        verify(importService, never()).refreshMovie(155L);

        verify(trackingService).recordSuccess(firstExternalId.getId());
        verify(trackingService, never()).recordFailure(secondExternalId.getId());
        verify(trackingService, never()).recordPermanentFailure(secondExternalId.getId());
        verify(cooldownService).recordRateLimit("TMDB");
    }

    @Test
    void skipsSynchronizationWhenProviderCooldownIsActive() {

        // Arrange: create mocked dependencies.
        MediaRepository mediaRepository = mock(MediaRepository.class);
        MediaExternalIdRepository externalIdRepository =
                mock(MediaExternalIdRepository.class);
        TmdbImportService importService = mock(TmdbImportService.class);
        SyncTrackingService trackingService = mock(SyncTrackingService.class);
        ProviderCooldownService cooldownService =
                mock(ProviderCooldownService.class);

        TmdbSyncService syncService = new TmdbSyncService(
                mediaRepository,
                externalIdRepository,
                importService,
                trackingService,
                cooldownService
        );

        // Simulate an active TMDB cooldown.
        when(cooldownService.isProviderAvailable("TMDB"))
                .thenReturn(false);

        // Act: attempt to synchronize 10 movies.
        int refreshedCount = syncService.syncOutdatedMovies(10);

        // Assert: the batch was skipped.
        assertThat(refreshedCount).isZero();

        verify(cooldownService).isProviderAvailable("TMDB");

        // The service must not query or refresh any movies.
        verifyNoInteractions(
                mediaRepository,
                externalIdRepository,
                importService,
                trackingService
        );
    }

    @Test
    void stopsBatchEvenWhenSavingCooldownFails() {

        MediaRepository mediaRepository = mock(MediaRepository.class);
        MediaExternalIdRepository externalIdRepository =
                mock(MediaExternalIdRepository.class);
        TmdbImportService importService = mock(TmdbImportService.class);
        SyncTrackingService trackingService = mock(SyncTrackingService.class);
        ProviderCooldownService cooldownService =
                mock(ProviderCooldownService.class);

        TmdbSyncService syncService = new TmdbSyncService(
                mediaRepository,
                externalIdRepository,
                importService,
                trackingService,
                cooldownService
        );

        Media firstMovie = new Media("Inception", MediaType.MOVIE);
        Media secondMovie = new Media("The Dark Knight", MediaType.MOVIE);

        ReflectionTestUtils.setField(firstMovie, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(secondMovie, "id", UUID.randomUUID());

        MediaExternalId externalId = new MediaExternalId(
                firstMovie, "TMDB", "MOVIE", "27205"
        );

        ReflectionTestUtils.setField(
                externalId, "id", UUID.randomUUID()
        );

        when(cooldownService.isProviderAvailable("TMDB"))
                .thenReturn(true);

        when(mediaRepository.findMediaNeedingSync(
                eq(MediaType.MOVIE),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(List.of(firstMovie, secondMovie))
        );

        when(externalIdRepository.findByMediaIdAndProviderAndProviderMediaType(
                eq(firstMovie.getId()), eq("TMDB"), eq("MOVIE")
        )).thenReturn(Optional.of(externalId));

        // TMDB rejects the first movie due to rate limiting.
        doThrow(new TmdbRateLimitException("TMDB rate limit exceeded"))
                .when(importService)
                .refreshMovie(27205L);

        // Simulate PostgreSQL failing to save the cooldown.
        doThrow(new IllegalStateException("Database write failed"))
                .when(cooldownService)
                .recordRateLimit("TMDB");

        int refreshedCount = syncService.syncOutdatedMovies(2);

        assertThat(refreshedCount).isZero();

        verify(importService).refreshMovie(27205L);

        // The second movie must NOT be requested.
        verify(importService, never()).refreshMovie(155L);

        verify(cooldownService).recordRateLimit("TMDB");

        verifyNoInteractions(trackingService);
    }

    @Test
    void stopsBatchWhenTmdbAuthenticationFails() {

        MediaRepository mediaRepository = mock(MediaRepository.class);
        MediaExternalIdRepository externalIdRepository =
                mock(MediaExternalIdRepository.class);
        TmdbImportService importService = mock(TmdbImportService.class);
        SyncTrackingService trackingService = mock(SyncTrackingService.class);
        ProviderCooldownService cooldownService =
                mock(ProviderCooldownService.class);

        TmdbSyncService syncService = new TmdbSyncService(
                mediaRepository,
                externalIdRepository,
                importService,
                trackingService,
                cooldownService
        );

        Media firstMovie = new Media("Inception", MediaType.MOVIE);
        Media secondMovie = new Media("The Dark Knight", MediaType.MOVIE);

        ReflectionTestUtils.setField(firstMovie, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(secondMovie, "id", UUID.randomUUID());

        MediaExternalId externalId = new MediaExternalId(
                firstMovie, "TMDB", "MOVIE", "27205"
        );

        ReflectionTestUtils.setField(
                externalId, "id", UUID.randomUUID()
        );

        when(cooldownService.isProviderAvailable("TMDB"))
                .thenReturn(true);

        when(mediaRepository.findMediaNeedingSync(
                eq(MediaType.MOVIE),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(List.of(firstMovie, secondMovie))
        );

        when(externalIdRepository.findByMediaIdAndProviderAndProviderMediaType(
                firstMovie.getId(), "TMDB", "MOVIE"
        )).thenReturn(Optional.of(externalId));

        // Simulate TMDB rejecting our API credentials.
        doThrow(new TmdbAuthenticationException("Invalid TMDB token"))
                .when(importService)
                .refreshMovie(27205L);

        int refreshedCount = syncService.syncOutdatedMovies(2);

        // No movies were refreshed successfully.
        assertThat(refreshedCount).isZero();

        verify(importService).refreshMovie(27205L);

        // The second movie must never be processed.
        verify(externalIdRepository, never())
                .findByMediaIdAndProviderAndProviderMediaType(
                        secondMovie.getId(), "TMDB", "MOVIE"
                );

        // Authentication errors must not affect individual retry tracking.
        verifyNoInteractions(trackingService);

        // HTTP 401 is not a rate-limit error.
        verify(cooldownService, never()).recordRateLimit("TMDB");
    }

    @Test
    void rejectsInvalidBatchSizeBeforeCheckingProviderCooldown() {

        MediaRepository mediaRepository = mock(MediaRepository.class);
        MediaExternalIdRepository externalIdRepository =
                mock(MediaExternalIdRepository.class);
        TmdbImportService importService = mock(TmdbImportService.class);
        SyncTrackingService trackingService = mock(SyncTrackingService.class);
        ProviderCooldownService cooldownService =
                mock(ProviderCooldownService.class);

        TmdbSyncService syncService = new TmdbSyncService(
                mediaRepository,
                externalIdRepository,
                importService,
                trackingService,
                cooldownService
        );

        // Invalid batch size must be rejected immediately.
        assertThatThrownBy(() -> syncService.syncOutdatedMovies(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Batch size must be between 1 and 100");

        // No provider checks, database queries, or API calls should occur.
        verifyNoInteractions(
                cooldownService,
                mediaRepository,
                externalIdRepository,
                importService,
                trackingService
        );
    }

}
