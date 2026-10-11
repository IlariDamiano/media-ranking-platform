
package com.ilaridamiano.backend.catalog.repository;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MediaPaginationRepositoryTest {

    private final MediaRepository mediaRepository;

    @Autowired
    MediaPaginationRepositoryTest(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    @Test
    void paginatesAndFiltersMovies() {

        mediaRepository.saveAndFlush(
                new Media("Movie A", MediaType.MOVIE)
        );
        mediaRepository.saveAndFlush(
                new Media("Movie B", MediaType.MOVIE)
        );
        mediaRepository.saveAndFlush(
                new Media("Anime C", MediaType.ANIME)
        );

        PageRequest pageable = PageRequest.of(
                0,
                1,
                Sort.by("title").ascending()
                        .and(Sort.by("id").ascending())
        );

        Page<Media> firstPage = mediaRepository.findByMediaType(
                MediaType.MOVIE,
                pageable
        );

        assertThat(firstPage.getContent()).hasSize(1);
        assertThat(firstPage.getContent().getFirst().getTitle())
                .isEqualTo("Movie A");

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.isLast()).isFalse();

        Page<Media> secondPage = mediaRepository.findByMediaType(
                MediaType.MOVIE,
                pageable.next()
        );

        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.getContent().getFirst().getTitle())
                .isEqualTo("Movie B");

        assertThat(secondPage.isLast()).isTrue();
    }
}
