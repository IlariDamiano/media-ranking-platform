
package com.ilaridamiano.backend.lists;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import com.ilaridamiano.backend.catalog.repository.MediaRepository;
import com.ilaridamiano.backend.library.repository.UserMediaEntryRepository;
import com.ilaridamiano.backend.lists.entity.*;
import com.ilaridamiano.backend.lists.repository.*;
import com.ilaridamiano.backend.user.entity.AppUser;
import com.ilaridamiano.backend.user.repository.AppUserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MediaListRepositoryIntegrationTest {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private MediaListRepository listRepository;

    @Autowired
    private MediaListItemRepository itemRepository;

    @Autowired
    private UserMediaEntryRepository libraryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void savesOrderedListsIndependentlyOfPersonalLibrary() {

        String uniqueName = "user_" +
                UUID.randomUUID().toString().substring(0, 8);

        AppUser user = userRepository.saveAndFlush(
                new AppUser(
                        uniqueName,
                        uniqueName + "@example.com",
                        passwordEncoder.encode("TestPassword123!")
                )
        );

        Media movie = mediaRepository.saveAndFlush(
                new Media("Interstellar", MediaType.MOVIE)
        );

        Media manga = mediaRepository.saveAndFlush(
                new Media("Berserk", MediaType.MANGA)
        );

        MediaList favorites = listRepository.saveAndFlush(
                new MediaList(
                        user,
                        "My Favorites",
                        "My all-time favorites"
                )
        );

        MediaList recommendations = listRepository.saveAndFlush(
                new MediaList(
                        user,
                        "Recommendations",
                        null
                )
        );

        // Add items intentionally in a different order.
        itemRepository.saveAndFlush(
                new MediaListItem(favorites, manga, 1)
        );

        itemRepository.saveAndFlush(
                new MediaListItem(favorites, movie, 0)
        );

        // The same media can appear in another list.
        itemRepository.saveAndFlush(
                new MediaListItem(recommendations, manga, 0)
        );

        assertThat(favorites.getVisibility())
                .isEqualTo(ListVisibility.PRIVATE);

        assertThat(itemRepository
                .findByMediaList_IdOrderByPositionAscIdAsc(
                        favorites.getId()
                ))
                .extracting(item -> item.getMedia().getId())
                .containsExactly(movie.getId(), manga.getId());

        assertThat(itemRepository.countByMediaList_Id(
                favorites.getId()
        )).isEqualTo(2);

        assertThat(listRepository.findByIdAndOwner_Id(
                favorites.getId(),
                user.getId()
        )).isPresent();

        // Nothing was added to the user's tracking library.
        assertThat(libraryRepository.existsByUser_IdAndMedia_Id(
                user.getId(),
                movie.getId()
        )).isFalse();

        assertThat(libraryRepository.existsByUser_IdAndMedia_Id(
                user.getId(),
                manga.getId()
        )).isFalse();
    }
}
