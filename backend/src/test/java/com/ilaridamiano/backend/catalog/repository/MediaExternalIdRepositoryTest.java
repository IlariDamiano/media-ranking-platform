
package com.ilaridamiano.backend.catalog.repository;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.entity.MediaExternalId;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MediaExternalIdRepositoryTest {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MediaExternalIdRepository externalIdRepository;

    @Test
    @Transactional
    void canSaveAndFindExternalId() {

        // Arrange: create and save a movie
        Media movie = new Media("Interstellar", MediaType.MOVIE);
        Media savedMovie = mediaRepository.saveAndFlush(movie);

        // Create its TMDB external identifier
        MediaExternalId externalId = new MediaExternalId(
                savedMovie,
                "TMDB",
                "MOVIE",
                "157336"
        );

        // Act: save the external identifier
        externalIdRepository.saveAndFlush(externalId);
        entityManager.clear();

        // Search using the provider identity
        MediaExternalId found = externalIdRepository
                .findByProviderAndProviderMediaTypeAndExternalId(
                        "TMDB",
                        "MOVIE",
                        "157336"
                )
                .orElseThrow();
        assertThat(Hibernate.isInitialized(found.getMedia()))
                .isTrue();
        // Assert
        assertThat(found.getId()).isNotNull();
        assertThat(found.getId().version()).isEqualTo(7);
        assertThat(found.getExternalId()).isEqualTo("157336");
        assertThat(found.getMedia().getId()).isEqualTo(savedMovie.getId());
    }
}
