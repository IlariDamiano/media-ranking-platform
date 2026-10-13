
package com.ilaridamiano.backend.user.profile;

import com.ilaridamiano.backend.user.entity.AppUser;
import com.ilaridamiano.backend.user.entity.ProfileVisibility;
import com.ilaridamiano.backend.user.entity.UserProfile;
import com.ilaridamiano.backend.user.profile.dto.PublicProfileResponse;
import com.ilaridamiano.backend.user.repository.AppUserRepository;
import com.ilaridamiano.backend.user.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;
import com.ilaridamiano.backend.user.profile.dto.UpdateMyProfileRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.ilaridamiano.backend.user.profile.dto.MyProfileResponse;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final AppUserRepository userRepository;
    private final UserProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(String username) {

        AppUser user = userRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(() -> profileNotFound());

        UserProfile profile = profileRepository
                .findById(user.getId())
                .orElseThrow(() -> profileNotFound());

        if (profile.getVisibility() != ProfileVisibility.PUBLIC) {
            throw profileNotFound();
        }

        return new PublicProfileResponse(
                user.getUsername(),
                profile.getBio(),
                user.getCreatedAt()
        );
    }

    private ResponseStatusException profileNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Profile not found"
        );
    }


    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(String email) {

        AppUser user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Account unavailable"
                ));

        UserProfile profile = profileRepository
                .findById(user.getId())
                .orElseThrow(() -> profileNotFound());

        return new MyProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                profile.getBio(),
                profile.getVisibility(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public MyProfileResponse updateMyProfile(
            String email,
            UpdateMyProfileRequest request
    ) {
        AppUser user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Account unavailable"
                ));

        UserProfile profile = profileRepository
                .findById(user.getId())
                .orElseThrow(() -> profileNotFound());

        String bio = request.bio();

        if (bio != null) {
            bio = bio.trim();

            if (bio.isEmpty()) {
                bio = null;
            }
        }

        profile.updateProfile(bio, request.visibility());

        return new MyProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                profile.getBio(),
                profile.getVisibility(),
                user.getCreatedAt()
        );
    }

}
