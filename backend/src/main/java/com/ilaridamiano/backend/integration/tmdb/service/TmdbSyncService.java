
package com.ilaridamiano.backend.integration.tmdb.service;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.entity.MediaExternalId;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import com.ilaridamiano.backend.catalog.repository.MediaExternalIdRepository;
import com.ilaridamiano.backend.catalog.repository.MediaRepository;
import com.ilaridamiano.backend.integration.tmdb.exception.TmdbPermanentException;
import com.ilaridamiano.backend.integration.tmdb.exception.TmdbRateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.ilaridamiano.backend.integration.sync.service.ProviderCooldownService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import com.ilaridamiano.backend.integration.tmdb.exception.TmdbAuthenticationException;
import com.ilaridamiano.backend.integration.sync.service.ProviderSyncLeaseService;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TmdbSyncService {

    private final MediaRepository mediaRepository;
    private final MediaExternalIdRepository externalIdRepository;
    private final TmdbImportService tmdbImportService;
    private final SyncTrackingService syncTrackingService;
    private final ProviderCooldownService providerCooldownService;
    private final ProviderSyncLeaseService providerSyncLeaseService;

    public Page<Media> findOutdatedMovies(int batchSize) {

        validateBatchSize(batchSize);

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

        validateBatchSize(batchSize);

        if (!providerCooldownService.isProviderAvailable("TMDB")) {
            log.info("Skipping TMDB sync: provider cooldown is active");
            return 0;
        }

        UUID leaseOwner = UUID.randomUUID();

        if (!providerSyncLeaseService.tryAcquire("TMDB", leaseOwner)) {
            log.info("Skipping TMDB sync: lease is unavailable");
            return 0;
        }

        try {
            return processOutdatedMovies(batchSize);

        } finally {
            try {
                boolean released = providerSyncLeaseService.release(
                        "TMDB",
                        leaseOwner
                );

                if (!released) {
                    log.warn("TMDB sync lease could not be released by its owner");
                }

            } catch (Exception exception) {
                log.error("Failed to release TMDB synchronization lease", exception);
            }
        }
    }

    private int processOutdatedMovies(int batchSize) {

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
            boolean permanentFailure = false;

            try {
                Long tmdbId = Long.valueOf(externalId.getExternalId());

                tmdbImportService.refreshMovie(tmdbId);

                successfulRefreshes++;
                refreshSucceeded = true;

            } catch (TmdbPermanentException exception) {
                permanentFailure = true;

                log.error(
                        "TMDB movie permanently unavailable for media {}",
                        movie.getId(),
                        exception
                );

            } catch (TmdbAuthenticationException exception) {

                log.error(
                        "TMDB authentication failed while synchronizing media {}. Stopping batch.",
                        movie.getId(),
                        exception
                );

                break;

            } catch (TmdbRateLimitException exception) {

                log.warn(
                        "TMDB rate limit reached while synchronizing media {}. Stopping batch.",
                        movie.getId(),
                        exception
                );

                try {
                    providerCooldownService.recordRateLimit("TMDB");

                } catch (Exception trackingException) {
                    log.error(
                            "Failed to persist TMDB provider cooldown",
                            trackingException
                    );
                }

                break;



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

                } else if (permanentFailure) {
                    syncTrackingService.recordPermanentFailure(
                            externalId.getId()
                    );

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

    private static void validateBatchSize(int batchSize) {
        if (batchSize < 1 || batchSize > 100) {
            throw new IllegalArgumentException(
                    "Batch size must be between 1 and 100"
            );
        }
    }
}
