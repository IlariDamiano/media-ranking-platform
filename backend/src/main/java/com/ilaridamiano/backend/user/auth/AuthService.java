
package com.ilaridamiano.backend.user.auth;

import com.ilaridamiano.backend.user.entity.AppUser;
import com.ilaridamiano.backend.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.ilaridamiano.backend.user.entity.UserProfile;
import com.ilaridamiano.backend.user.repository.UserProfileRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final UserProfileRepository profileRepository;

    @Transactional
    public AppUser register(RegisterRequest request) {

        String username = request.username().trim();
        String email = request.email().trim()
                .toLowerCase(Locale.ROOT);

        if (repository.existsByEmailIgnoreCase(email)
                || repository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username or email already registered"
            );
        }

        // BCrypt accepts passwords of at most 72 bytes.
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password is too long"
            );
        }

        String hash = passwordEncoder.encode(request.password());

        AppUser user = new AppUser(username, email, hash);

        // Persist the account first so it receives its UUID.
        AppUser savedUser = repository.save(user);

        // Create the matching public profile.
        UserProfile profile = new UserProfile(savedUser);
        profileRepository.save(profile);

        return savedUser;

    }


    @Transactional(readOnly = true)
    public AppUser getCurrentUser(String email) {
        return repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Account is unavailable"
                ));
    }

}
