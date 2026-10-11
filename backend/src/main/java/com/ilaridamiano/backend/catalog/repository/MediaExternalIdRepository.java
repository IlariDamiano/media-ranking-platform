
package com.ilaridamiano.backend.catalog.repository;

import com.ilaridamiano.backend.catalog.entity.MediaExternalId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MediaExternalIdRepository
        extends JpaRepository<MediaExternalId, UUID> {

    @EntityGraph(attributePaths = "media")
    Optional<MediaExternalId> findByProviderAndProviderMediaTypeAndExternalId(
            String provider,
            String providerMediaType,
            String externalId
    );

    Optional<MediaExternalId> findByMediaIdAndProviderAndProviderMediaType(
            UUID mediaId,
            String provider,
            String providerMediaType
    );

}
