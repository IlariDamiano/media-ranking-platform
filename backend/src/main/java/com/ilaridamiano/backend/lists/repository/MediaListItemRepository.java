
package com.ilaridamiano.backend.lists.repository;

import com.ilaridamiano.backend.lists.entity.MediaListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaListItemRepository
        extends JpaRepository<MediaListItem, UUID> {

    @EntityGraph(attributePaths = "media")
    List<MediaListItem> findByMediaList_IdOrderByPositionAscIdAsc(
            UUID listId
    );

    @Query("""
        SELECT MAX(i.position)
        FROM MediaListItem i
        WHERE i.mediaList.id = :listId
        """)
    Integer findMaxPosition(@Param("listId") UUID listId);

    Optional<MediaListItem> findByMediaList_IdAndMedia_Id(
            UUID listId,
            UUID mediaId
    );

    boolean existsByMediaList_IdAndMedia_Id(
            UUID listId,
            UUID mediaId
    );

    long countByMediaList_Id(UUID listId);
}
