
package com.ilaridamiano.backend.lists;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import com.ilaridamiano.backend.catalog.repository.MediaRepository;
import com.ilaridamiano.backend.library.repository.UserMediaEntryRepository;
import com.ilaridamiano.backend.lists.entity.*;
import com.ilaridamiano.backend.lists.repository.*;
import com.ilaridamiano.backend.user.entity.AppUser;
import com.ilaridamiano.backend.user.repository.AppUserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaListApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private MediaListRepository lists;

    @Autowired
    private MediaListItemRepository items;

    @Autowired
    private UserMediaEntryRepository personalLibrary;

    private AppUser alice;
    private AppUser bob;

    private UUID movieA;
    private UUID movieB;

    @BeforeEach
    void setUp() {

        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        // Authentication is simulated in these API tests,
        // so these accounts don't need usable passwords.
        alice = users.saveAndFlush(new AppUser(
                "alice_" + suffix,
                "alice_" + suffix + "@example.com",
                "test-only-hash"
        ));

        bob = users.saveAndFlush(new AppUser(
                "bob_" + suffix,
                "bob_" + suffix + "@example.com",
                "test-only-hash"
        ));

        movieA = mediaRepository.saveAndFlush(
                new Media("Movie A " + suffix, MediaType.MOVIE)
        ).getId();

        movieB = mediaRepository.saveAndFlush(
                new Media("Movie B " + suffix, MediaType.MOVIE)
        ).getId();
    }

    @AfterEach
    void cleanUp() {

        // Deleting the owners cascades to their lists
        // and list items via our foreign keys.
        users.deleteById(alice.getId());
        users.deleteById(bob.getId());

        mediaRepository.deleteById(movieA);
        mediaRepository.deleteById(movieB);
    }

    @Test
    void publicListsAreAnonymousButPrivateListsAreProtected()
            throws Exception {

        MediaList publicList = createList(alice, true);
        MediaList privateList = createList(alice, false);

        items.saveAndFlush(new MediaListItem(
                publicList,
                mediaRepository.findById(movieA).orElseThrow(),
                0
        ));

        mvc.perform(get("/api/lists/{id}", publicList.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.list.visibility")
                        .value("PUBLIC"))
                .andExpect(jsonPath("$.items[0].mediaId")
                        .value(movieA.toString()));

        mvc.perform(get("/api/lists/{id}", privateList.getId()))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/lists/{id}", privateList.getId())
                        .with(user(bob.getEmail()).roles("USER")))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/lists/{id}", privateList.getId())
                        .with(user(alice.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.list.ownerUsername")
                        .value(alice.getUsername()));
    }

    @Test
    void ownerCanCreateAndUpdateLists() throws Exception {

        mvc.perform(post("/api/me/lists")
                        .with(user(alice.getEmail()).roles("USER"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                        {
                          "name": "Favorite Sci-Fi",
                          "description": "My rankings"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visibility")
                        .value("PRIVATE"));

        MediaList created = lists.findByOwner_Id(
                alice.getId(),
                PageRequest.of(0, 10)
        ).getContent().getFirst();

        mvc.perform(put("/api/me/lists/{id}", created.getId())
                        .with(user(alice.getEmail()).roles("USER"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                        {
                          "name": "Best Sci-Fi",
                          "description": null,
                          "visibility": "PUBLIC"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Best Sci-Fi"))
                .andExpect(jsonPath("$.visibility")
                        .value("PUBLIC"));

        mvc.perform(get("/api/lists/{id}", created.getId()))
                .andExpect(status().isOk());

        mvc.perform(get("/api/me/lists")
                        .with(user(alice.getEmail()).roles("USER")))
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(get("/api/me/lists")
                        .with(user(bob.getEmail()).roles("USER")))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void onlyOwnerCanModifyLists() throws Exception {

        MediaList list = createList(alice, false);

        mvc.perform(put("/api/me/lists/{id}", list.getId())
                        .with(user(bob.getEmail()).roles("USER"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                        {
                          "name": "Stolen List",
                          "visibility": "PUBLIC"
                        }
                        """))
                .andExpect(status().isNotFound());

        addItemAs(bob, list.getId(), movieA)
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/me/lists/{id}", list.getId())
                        .with(user(bob.getEmail()).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        mvc.perform(put("/api/me/lists/{id}/order", list.getId())
                        .with(user(bob.getEmail()).roles("USER"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                        {"mediaIds":[]}
                        """))
                .andExpect(status().isNotFound());

        assertThat(lists.existsById(list.getId())).isTrue();
    }

    @Test
    void canAddReorderAndRemoveMedia() throws Exception {

        MediaList list = createList(alice, false);

        addItemAs(alice, list.getId(), movieA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(0));

        addItemAs(alice, list.getId(), movieB)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(1));

        addItemAs(alice, list.getId(), movieA)
                .andExpect(status().isConflict());

        String reversedOrder = """
                {"mediaIds":["%s","%s"]}
                """.formatted(movieB, movieA);

        mvc.perform(put("/api/me/lists/{id}/order", list.getId())
                        .with(user(alice.getEmail()).roles("USER"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(reversedOrder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mediaId")
                        .value(movieB.toString()))
                .andExpect(jsonPath("$[1].mediaId")
                        .value(movieA.toString()));

        List<UUID> savedOrder = items
                .findByMediaList_IdOrderByPositionAscIdAsc(list.getId())
                .stream()
                .map(item -> item.getMedia().getId())
                .toList();

        assertThat(savedOrder)
                .containsExactly(movieB, movieA);

        mvc.perform(delete(
                        "/api/me/lists/{id}/items/{mediaId}",
                        list.getId(),
                        movieA
                )
                        .with(user(alice.getEmail()).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(items.countByMediaList_Id(list.getId()))
                .isEqualTo(1);

        // Lists do not automatically add tracked media.
        assertThat(personalLibrary.existsByUser_IdAndMedia_Id(
                alice.getId(), movieB
        )).isFalse();
    }

    @Test
    void rejectsInvalidReorderRequests() throws Exception {

        MediaList list = createList(alice, false);

        addItemAs(alice, list.getId(), movieA)
                .andExpect(status().isCreated());

        addItemAs(alice, list.getId(), movieB)
                .andExpect(status().isCreated());

        String duplicateOrder = """
                {"mediaIds":["%s","%s"]}
                """.formatted(movieA, movieA);

        mvc.perform(put("/api/me/lists/{id}/order", list.getId())
                        .with(user(alice.getEmail()).roles("USER"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(duplicateOrder))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/me/lists/{id}/order", list.getId())
                        .with(user(alice.getEmail()).roles("USER"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"mediaIds\":[\"" + movieA + "\"]}"))
                .andExpect(status().isBadRequest());

        assertThat(items
                .findByMediaList_IdOrderByPositionAscIdAsc(list.getId())
                .stream()
                .map(item -> item.getMedia().getId()))
                .containsExactly(movieA, movieB);
    }

    @Test
    void requiresAuthenticationAndCsrfForWrites() throws Exception {

        mvc.perform(get("/api/me/lists"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/me/lists")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                        {"name":"Anonymous List"}
                        """))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/me/lists")
                        .with(user(alice.getEmail()).roles("USER"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                        {"name":"No CSRF"}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletingAListRemovesItsItems() throws Exception {

        MediaList list = createList(alice, false);

        addItemAs(alice, list.getId(), movieA)
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/me/lists/{id}", list.getId())
                        .with(user(alice.getEmail()).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(lists.existsById(list.getId())).isFalse();
        assertThat(items.countByMediaList_Id(list.getId()))
                .isZero();
    }

    @Test
    void concurrentAddsGetDifferentPositions() throws Exception {

        MediaList list = createList(alice, false);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Integer> first = executor.submit(() -> {
                start.await();
                return addItemAs(alice, list.getId(), movieA)
                        .andReturn().getResponse().getStatus();
            });

            Future<Integer> second = executor.submit(() -> {
                start.await();
                return addItemAs(alice, list.getId(), movieB)
                        .andReturn().getResponse().getStatus();
            });

            start.countDown();

            assertThat(first.get(20, TimeUnit.SECONDS))
                    .isEqualTo(201);

            assertThat(second.get(20, TimeUnit.SECONDS))
                    .isEqualTo(201);

            assertThat(items
                    .findByMediaList_IdOrderByPositionAscIdAsc(list.getId())
                    .stream()
                    .map(MediaListItem::getPosition))
                    .containsExactly(0, 1);

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private MediaList createList(
            AppUser owner,
            boolean isPublic
    ) {
        MediaList list = new MediaList(
                owner,
                "Test Favorites",
                "Integration test collection"
        );

        if (isPublic) {
            list.updateDetails(
                    list.getName(),
                    list.getDescription(),
                    ListVisibility.PUBLIC
            );
        }

        return lists.saveAndFlush(list);
    }

    private ResultActions addItemAs(
            AppUser account,
            UUID listId,
            UUID mediaId
    ) throws Exception {

        return mvc.perform(post("/api/me/lists/{id}/items", listId)
                .with(user(account.getEmail()).roles("USER"))
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content("{\"mediaId\":\"" + mediaId + "\"}"));
    }
}
