
package com.ilaridamiano.backend.integration.tmdb.service;

import com.ilaridamiano.backend.catalog.repository.MediaRepository;
import com.ilaridamiano.backend.catalog.repository.MediaExternalIdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ilaridamiano.backend.catalog.entity.MediaExternalId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TmdbSyncService {

    private final MediaRepository mediaRepository;
    private final MediaExternalIdRepository externalIdRepository;
    private final TmdbImportService tmdbImportService;
    private final SyncTrackingService syncTrackingService;

    public Page<Media> findOutdatedMovies(int batchSize) {

        if (batchSize < 1 || batchSize > 100) {
            throw new IllegalArgumentException(
                    "Batch size must be between 1 and 100"
            );
        }

        Instant now = Instant.now();
        Instant cutoff = now.minus(7, ChronoUnit.DAYS);

        PageRequest pageable = PageRequest.of(
                0,
                batchSize,
                Sort.by(
                        Sort.Order.asc("lastSyncedAt").nullsFirst(),
                        Sort.Order.asc("id")
                )
        );

        return mediaRepository.findMediaNeedingSync(
                MediaType.MOVIE,
                cutoff,
                now,
                pageable
        );
    }

    public int syncOutdatedMovies(int batchSize) {

        Page<Media> batch = findOutdatedMovies(batchSize);

        int successfulRefreshes = 0;

        for (Media movie : batch.getContent()) {

            MediaExternalId externalId;

            try {
                externalId = externalIdRepository
                        .findByMediaIdAndProviderAndProviderMediaType(
                                movie.getId(),
                                "TMDB",
                                "MOVIE"
                        )
                        .orElseThrow();

            } catch (Exception exception) {
                log.error(
                        "Could not resolve TMDB external ID for media {}",
                        movie.getId(),
                        exception
                );
                continue;
            }

            boolean refreshSucceeded = false;

            try {
                Long tmdbId = Long.valueOf(externalId.getExternalId());

                tmdbImportService.refreshMovie(tmdbId);

                successfulRefreshes++;
                refreshSucceeded = true;

            } catch (Exception exception) {
                log.error(
                        "TMDB synchronization failed for media {}",
                        movie.getId(),
                        exception
                );
            }

            try {
                if (refreshSucceeded) {
                    syncTrackingService.recordSuccess(externalId.getId());
                } else {
                    syncTrackingService.recordFailure(externalId.getId());
                }

            } catch (Exception exception) {
                log.error(
                        "Could not update synchronization tracking for media {}",
                        movie.getId(),
                        exception
                );
            }
        }

        return successfulRefreshes;
    }



}
