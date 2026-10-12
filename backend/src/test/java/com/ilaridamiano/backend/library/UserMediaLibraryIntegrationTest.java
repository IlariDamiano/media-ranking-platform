
package com.ilaridamiano.backend.library;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import com.ilaridamiano.backend.catalog.repository.MediaRepository;
import com.ilaridamiano.backend.library.repository.UserMediaEntryRepository;
import com.ilaridamiano.backend.user.entity.AppUser;
import com.ilaridamiano.backend.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserMediaLibraryIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private UserMediaEntryRepository entries;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void preservesRatingWhenOnlyStatusChanges() throws Exception {

        AppUser alice = createUser("alice");
        UUID movieId = createMovie("Interstellar");

        saveAs(alice, movieId, """
                {"status":"IN_PROGRESS","rating":9.3}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(9.3));

        saveAs(alice, movieId, """
                {"status":"COMPLETED"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.rating").value(9.3));

        assertThat(entries.findByUser_Id(
                alice.getId(), PageRequest.of(0, 10)
        ).getTotalElements()).isEqualTo(1);

        mvc.perform(delete("/api/me/media/{mediaId}/rating", movieId)
                        .with(user(alice.getEmail()).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/me/media/{mediaId}", movieId)
                        .with(user(alice.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.rating").value(nullValue()));
    }

    @Test
    void keepsDifferentUsersLibrariesIsolated() throws Exception {

        AppUser alice = createUser("alice");
        AppUser bob = createUser("bob");
        UUID movieId = createMovie("Inception");

        saveAs(alice, movieId, """
                {"status":"COMPLETED","rating":9.5}
                """).andExpect(status().isOk());

        mvc.perform(get("/api/me/media/{mediaId}", movieId)
                        .with(user(bob.getEmail()).roles("USER")))
                .andExpect(status().isNotFound());

        saveAs(bob, movieId, """
                {"status":"PLANNED","rating":7.2}
                """).andExpect(status().isOk());

        mvc.perform(get("/api/me/media/{mediaId}", movieId)
                        .with(user(alice.getEmail()).roles("USER")))
                .andExpect(jsonPath("$.rating").value(9.5));

        mvc.perform(get("/api/me/media/{mediaId}", movieId)
                        .with(user(bob.getEmail()).roles("USER")))
                .andExpect(jsonPath("$.rating").value(7.2));

        mvc.perform(delete("/api/me/media/{mediaId}", movieId)
                        .with(user(bob.getEmail()).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/me/media/{mediaId}", movieId)
                        .with(user(alice.getEmail()).roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidRatingsAndUnauthenticatedWrites()
            throws Exception {

        AppUser alice = createUser("alice");
        UUID movieId = createMovie("Test Movie");

        for (String rating : List.of("8.75", "0.9", "10.1")) {
            saveAs(alice, movieId,
                    "{\"status\":\"PLANNED\",\"rating\":" + rating + "}")
                    .andExpect(status().isBadRequest());
        }

        saveAs(alice, movieId, """
                {"rating":8.7}
                """).andExpect(status().isBadRequest());

        mvc.perform(put("/api/me/media/{mediaId}", movieId)
                        .with(user(alice.getEmail()).roles("USER"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                    {"status":"COMPLETED","rating":8.7}
                    """))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/me/media"))
                .andExpect(status().isUnauthorized());

        mvc.perform(put("/api/me/media/{mediaId}", movieId)
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                    {"status":"COMPLETED"}
                    """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void filtersAndPaginatesPersonalEntries() throws Exception {

        AppUser alice = createUser("alice");
        UUID planned = createMovie("Planned Movie");
        UUID completed = createMovie("Completed Movie");

        saveAs(alice, planned, """
                {"status":"PLANNED"}
                """).andExpect(status().isOk());

        saveAs(alice, completed, """
                {"status":"COMPLETED","rating":8.7}
                """).andExpect(status().isOk());

        mvc.perform(get("/api/me/media")
                        .with(user(alice.getEmail()).roles("USER"))
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].mediaId")
                        .value(completed.toString()));

        mvc.perform(get("/api/me/media")
                        .with(user(alice.getEmail()).roles("USER"))
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.number").value(1));

        mvc.perform(get("/api/me/media")
                        .with(user(alice.getEmail()).roles("USER"))
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    private AppUser createUser(String prefix) {

        String unique = prefix +
                UUID.randomUUID().toString().substring(0, 8);

        return users.saveAndFlush(new AppUser(
                unique,
                unique + "@example.com",
                passwordEncoder.encode("TestPassword123!")
        ));
    }

    private UUID createMovie(String title) {
        return mediaRepository.saveAndFlush(
                new Media(title, MediaType.MOVIE)
        ).getId();
    }

    private ResultActions saveAs(
            AppUser account,
            UUID mediaId,
            String json
    ) throws Exception {

        return mvc.perform(put("/api/me/media/{mediaId}", mediaId)
                .with(user(account.getEmail()).roles("USER"))
                .with(csrf())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(json));
    }
}
