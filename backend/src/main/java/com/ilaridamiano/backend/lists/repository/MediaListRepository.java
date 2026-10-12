
package com.ilaridamiano.backend.lists.repository;

import com.ilaridamiano.backend.lists.entity.ListVisibility;
import com.ilaridamiano.backend.lists.entity.MediaList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface MediaListRepository
        extends JpaRepository<MediaList, UUID> {

    Page<MediaList> findByOwner_Id(
            UUID ownerId,
            Pageable pageable
    );

    Optional<MediaList> findByIdAndOwner_Id(
            UUID listId,
            UUID ownerId
    );

    Page<MediaList> findByVisibility(
            ListVisibility visibility,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT l FROM MediaList l
        WHERE l.id = :listId
          AND l.owner.id = :ownerId
        """)
    Optional<MediaList> findOwnedForUpdate(
            @Param("listId") UUID listId,
            @Param("ownerId") UUID ownerId
    );
}
