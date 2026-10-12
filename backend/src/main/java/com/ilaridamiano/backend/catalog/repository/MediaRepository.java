package com.ilaridamiano.backend.catalog.repository;

import com.ilaridamiano.backend.catalog.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface MediaRepository extends JpaRepository<Media, UUID> {
    List<Media> findByTitleContainingIgnoreCase(String title);

    @Query(value = """
            SELECT m.*
        FROM
            media m
            WHERE
            lower(m.title) LIKE concat('%', lower(:query), '%')
            OR lower(m.title) % lower(:query)
            OR (
                length(trim(:query)) >= 5
                AND word_similarity(
                    lower(trim(:query)),
                    lower(m.title)
                ) >= 0.32
            )           
            OR EXISTS (
                SELECT 1
                FROM media_aliases a
                WHERE a.media_id = m.id
                  AND (
                      lower(a.title) LIKE concat('%', lower(:query), '%')
                      OR lower(a.title) % lower(:query)
                      OR (
                          length(trim(:query)) >= 5
                          AND word_similarity(
                              lower(trim(:query)),
                              lower(a.title)
                          ) >= 0.32
                      )
                  )
            )
            ORDER BY
            CASE
                WHEN lower(m.title) = lower(:query) THEN 0
                WHEN EXISTS (
                    SELECT 1
                    FROM media_aliases a
                    WHERE a.media_id = m.id
                      AND lower(a.title) = lower(:query)
                ) THEN 1
                WHEN lower(m.title) LIKE concat(lower(:query), '%') THEN 2
                WHEN lower(m.title) LIKE concat('%', lower(:query), '%') THEN 3
                ELSE 4
            END,
            GREATEST(
                similarity(lower(m.title), lower(:query)),
            
                word_similarity(
                    lower(trim(:query)),
                    lower(m.title)
                ),
            
                COALESCE((
                    SELECT MAX(
                        GREATEST(
                            similarity(
                                lower(a.title),
                                lower(:query)
                            ),
                            word_similarity(
                                lower(trim(:query)),
                                lower(a.title)
                            )
                        )
                    )
                    FROM media_aliases a
                    WHERE a.media_id = m.id
                ), 0)
            ) DESC,
            m.title ASC,
            m.id ASC
            LIMIT 20
        """, nativeQuery = true)
    List<Media> searchByRelevance(String query);

    Page<Media> findByMediaType(
            MediaType mediaType,
            Pageable pageable
    );

    @Query("""
    SELECT m FROM Media m
    WHERE m.mediaType = :mediaType
    AND (m.lastSyncedAt IS NULL OR m.lastSyncedAt < :cutoff)
    AND EXISTS (
        SELECT e.id FROM MediaExternalId e
        WHERE e.media = m
        AND e.provider = 'TMDB'
        AND e.providerMediaType = 'MOVIE'
        AND e.syncDisabled = false
        AND (
            e.nextSyncAttemptAt IS NULL
            OR e.nextSyncAttemptAt <= :now
        )
    )
    """)
    Page<Media> findMediaNeedingSync(
            @Param("mediaType") MediaType mediaType,
            @Param("cutoff") Instant cutoff,
            @Param("now") Instant now,
            Pageable pageable
    );




}