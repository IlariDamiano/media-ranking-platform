
package com.ilaridamiano.backend.catalog.repository;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.entity.MediaAlias;
import com.ilaridamiano.backend.catalog.enums.MediaType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MediaAliasRepositoryTest {

    private final MediaRepository mediaRepository;
    private final MediaAliasRepository mediaAliasRepository;
    private final EntityManager entityManager;

    @Autowired
    MediaAliasRepositoryTest(
            MediaRepository mediaRepository,
            MediaAliasRepository mediaAliasRepository,
            EntityManager entityManager
    ) {
        this.mediaRepository = mediaRepository;
        this.mediaAliasRepository = mediaAliasRepository;
        this.entityManager = entityManager;
    }

    @Test
    void savesAndFindsMultilingualAliases() {

        Media anime = mediaRepository.saveAndFlush(
                new Media("Attack on Titan", MediaType.ANIME)
        );

        MediaAlias italianAlias = new MediaAlias(
                anime,
                "L'attacco dei giganti",
                "it",
                "ALTERNATIVE"
        );

        MediaAlias japaneseAlias = new MediaAlias(
                anime,
                "進撃の巨人",
                "ja",
                "ALTERNATIVE"
        );

        mediaAliasRepository.saveAndFlush(italianAlias);
        mediaAliasRepository.saveAndFlush(japaneseAlias);
        entityManager.flush();
        entityManager.clear();

        List<MediaAlias> aliases =
                mediaAliasRepository.findByMediaId(anime.getId());

        assertThat(aliases).hasSize(2);

        assertThat(aliases)
                .extracting(MediaAlias::getTitle)
                .containsExactlyInAnyOrder(
                        "L'attacco dei giganti",
                        "進撃の巨人"
                );
    }
}
