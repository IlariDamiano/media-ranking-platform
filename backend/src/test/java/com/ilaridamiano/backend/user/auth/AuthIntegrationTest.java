
package com.ilaridamiano.backend.user.auth;

import com.ilaridamiano.backend.user.entity.AppUser;
import com.ilaridamiano.backend.user.entity.UserRole;
import com.ilaridamiano.backend.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registrationHashesPasswordAndAssignsUserRole() throws Exception {

        String email = "register-" + UUID.randomUUID() + "@example.com";
        String password = "StrongPassword123!";

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "username": "TestUser",
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("TestUser"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        AppUser saved = users.findByEmailIgnoreCase(email)
                .orElseThrow();

        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        assertThat(saved.getPasswordHash()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(
                password, saved.getPasswordHash()
        )).isTrue();
    }

    @Test
    void rejectsDuplicateUsernameIgnoringCase() throws Exception {

        String email1 = "first-" + UUID.randomUUID() + "@example.com";
        String email2 = "second-" + UUID.randomUUID() + "@example.com";

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "username": "UniqueUser",
                      "email": "%s",
                      "password": "StrongPassword123!"
                    }
                    """.formatted(email1)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "username": "uniqueuser",
                      "email": "%s",
                      "password": "StrongPassword123!"
                    }
                    """.formatted(email2)))
                .andExpect(status().isConflict());
    }

    @Test
    void loginPersistsSessionAndLogoutInvalidatesIt() throws Exception {

        String email = "session-" + UUID.randomUUID() + "@example.com";
        String password = "StrongPassword123!";

        users.saveAndFlush(new AppUser(
                "SessionUser",
                email,
                passwordEncoder.encode(password)
        ));

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession)
                login.getRequest().getSession(false);

        assertThat(session).isNotNull();

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("SessionUser"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(csrf().asHeader()))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void rejectsInvalidLoginCredentials() throws Exception {

        mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "email": "unknown@example.com",
                      "password": "WrongPassword123!"
                    }
                    """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresCsrfForRegistration() throws Exception {

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "username": "MissingCsrf",
                      "email": "csrf@example.com",
                      "password": "StrongPassword123!"
                    }
                    """))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void keepsMediaPublicButProtectsPrivateAndAdminEndpoints()
            throws Exception {

        mvc.perform(get("/api/media").param("size", "1"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/media/import/tmdb/movies/1")
                        .with(user("reader").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/media/refresh/tmdb/movies/157336")
                        .with(user("reader").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

    }

    @Test
    void validatesMinimumPasswordLength() throws Exception {

        String id = UUID.randomUUID().toString();

        // Exactly 9 characters: accepted.
        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "username": "NineCharUser",
                          "email": "nine-%s@example.com",
                          "password": "abcdefghi"
                        }
                        """.formatted(id)))
                .andExpect(status().isCreated());

        // Only 8 characters: rejected.
        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "username": "EightCharUser",
                          "email": "eight-%s@example.com",
                          "password": "abcdefgh"
                        }
                        """.formatted(id)))
                .andExpect(status().isBadRequest());
    }

}
