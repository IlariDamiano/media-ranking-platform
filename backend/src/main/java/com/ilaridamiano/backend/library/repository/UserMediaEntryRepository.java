
package com.ilaridamiano.backend.library.repository;

import com.ilaridamiano.backend.library.entity.MediaProgressStatus;
import com.ilaridamiano.backend.library.entity.UserMediaEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserMediaEntryRepository
        extends JpaRepository<UserMediaEntry, UUID> {

    Optional<UserMediaEntry> findByUser_IdAndMedia_Id(
            UUID userId,
            UUID mediaId
    );

    Page<UserMediaEntry> findByUser_Id(
            UUID userId,
            Pageable pageable
    );

    Page<UserMediaEntry> findByUser_IdAndStatus(
            UUID userId,
            MediaProgressStatus status,
            Pageable pageable
    );

    boolean existsByUser_IdAndMedia_Id(
            UUID userId,
            UUID mediaId
    );
}
