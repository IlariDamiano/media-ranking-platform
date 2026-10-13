
package com.ilaridamiano.backend.user.profile;

import org.junit.jupiter.api.Test;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserProfileIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void publicProfileIsAccessibleWithoutLogin() throws Exception {

        String username = "ProfileTest";
        String email = "profile-" + UUID.randomUUID() + "@example.com";

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": "%s",
                              "email": "%s",
                              "password": "StrongPassword123!"
                            }
                            """.formatted(username, email)))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/profiles/profiletest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.joinedAt").isNotEmpty())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist());
    }

    @Test
    void nonexistentProfileReturnsNotFound() throws Exception {

        mvc.perform(get("/api/profiles/UnknownProfile123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void privateProfileRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/profile/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanReadOwnProfile() throws Exception {

        String username = "PrivateProfileTest";
        String email = "private-" + UUID.randomUUID() + "@example.com";

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "username": "%s",
                          "email": "%s",
                          "password": "StrongPassword123!"
                        }
                        """.formatted(username, email)))
                .andExpect(status().isCreated());

        // Simulate the authenticated account.
        mvc.perform(get("/api/profile/me")
                        .with(user(email).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.joinedAt").isNotEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // Public visitors must not see the email.
        mvc.perform(get("/api/profiles/" + username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void userCanEditProfileAndChangeVisibility() throws Exception {

        String email = "visibility-" + UUID.randomUUID() + "@example.com";

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "username": "VisibilityTest",
                          "email": "%s",
                          "password": "StrongPassword123!"
                        }
                        """.formatted(email)))
                .andExpect(status().isCreated());

        // Update biography while keeping the profile public.
        mvc.perform(patch("/api/profile/me")
                        .with(user(email).roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "bio": "  I love anime and novels!  ",
                          "visibility": "PUBLIC"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio")
                        .value("I love anime and novels!"))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));

        // Public visitors can read the updated biography.
        mvc.perform(get("/api/profiles/VisibilityTest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio")
                        .value("I love anime and novels!"));

        // Change profile visibility to private.
        mvc.perform(patch("/api/profile/me")
                        .with(user(email).roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "bio": "Private profile",
                          "visibility": "PRIVATE"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));

        // Public visitors can no longer access it.
        mvc.perform(get("/api/profiles/VisibilityTest"))
                .andExpect(status().isNotFound());

        // The owner can still access their own profile.
        mvc.perform(get("/api/profile/me")
                        .with(user(email).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Private profile"));
    }

    @Test
    void profileUpdatesRequireAuthenticationAndValidInput()
            throws Exception {

        String validUpdate = """
        {
          "bio": "My updated biography",
          "visibility": "PUBLIC"
        }
        """;

        // Unauthenticated visitors cannot edit profiles.
        mvc.perform(patch("/api/profile/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdate))
                .andExpect(status().isUnauthorized());

        // Authenticated requests still require CSRF.
        mvc.perform(patch("/api/profile/me")
                        .with(user("test@example.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdate))
                .andExpect(status().isForbidden());

        // Biographies longer than 500 characters are rejected.
        String longBio = "a".repeat(501);

        mvc.perform(patch("/api/profile/me")
                        .with(user("test@example.com").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "bio": "%s",
                          "visibility": "PUBLIC"
                        }
                        """.formatted(longBio)))
                .andExpect(status().isBadRequest());

        // Visibility cannot be omitted.
        mvc.perform(patch("/api/profile/me")
                        .with(user("test@example.com").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "bio": "Missing visibility"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

}
