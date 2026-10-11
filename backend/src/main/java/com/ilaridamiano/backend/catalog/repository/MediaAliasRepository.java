
package com.ilaridamiano.backend.catalog.repository;

import com.ilaridamiano.backend.catalog.entity.MediaAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaAliasRepository
        extends JpaRepository<MediaAlias, UUID> {

    List<MediaAlias> findByMediaId(UUID mediaId);
}
