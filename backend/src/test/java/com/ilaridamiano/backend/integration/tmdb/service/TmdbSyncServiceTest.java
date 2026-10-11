
package com.ilaridamiano.backend.integration.tmdb.service;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.entity.MediaExternalId;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import com.ilaridamiano.backend.catalog.repository.MediaExternalIdRepository;
import com.ilaridamiano.backend.catalog.repository.MediaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TmdbSyncServiceTest {

    @Test
    void refreshesSelectedMoviesAndReturnsSuccessCount() {

        MediaRepository mediaRepository = mock(MediaRepository.class);
        MediaExternalIdRepository externalIdRepository =
                mock(MediaExternalIdRepository.class);
        TmdbImportService importService = mock(TmdbImportService.class);
        SyncTrackingService trackingService = mock(SyncTrackingService.class);

        TmdbSyncService syncService = new TmdbSyncService(
                mediaRepository,
                externalIdRepository,
                importService,
                trackingService
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

        TmdbSyncService syncService = new TmdbSyncService(
                mediaRepository,
                externalIdRepository,
                importService,
                trackingService
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

        int refreshedCount = syncService.syncOutdatedMovies(2);

        assertThat(refreshedCount).isEqualTo(1);

        verify(importService).refreshMovie(157336L);
        verify(importService).refreshMovie(27205L);
        verify(trackingService).recordFailure(firstExternalId.getId());
        verify(trackingService).recordSuccess(secondExternalId.getId());
    }

}
