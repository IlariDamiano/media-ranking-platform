
package com.ilaridamiano.backend.library.repository;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import com.ilaridamiano.backend.catalog.repository.MediaRepository;
import com.ilaridamiano.backend.library.entity.MediaProgressStatus;
import com.ilaridamiano.backend.library.entity.UserMediaEntry;
import com.ilaridamiano.backend.user.entity.AppUser;
import com.ilaridamiano.backend.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserMediaEntryRepositoryTest {

    @Autowired
    private UserMediaEntryRepository entryRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void savesPersonalMediaEntryWithDecimalRating() {

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
                new Media("Test Movie", MediaType.MOVIE)
        );

        UserMediaEntry entry = new UserMediaEntry(
                user,
                movie,
                MediaProgressStatus.COMPLETED,
                new BigDecimal("8.7")
        );

        entryRepository.saveAndFlush(entry);

        UserMediaEntry saved = entryRepository
                .findByUser_IdAndMedia_Id(
                        user.getId(),
                        movie.getId()
                )
                .orElseThrow();

        assertThat(saved.getStatus())
                .isEqualTo(MediaProgressStatus.COMPLETED);

        assertThat(saved.getRating())
                .isEqualByComparingTo("8.7");

        assertThat(saved.getUser().getId())
                .isEqualTo(user.getId());

        assertThat(saved.getMedia().getId())
                .isEqualTo(movie.getId());
    }
}
