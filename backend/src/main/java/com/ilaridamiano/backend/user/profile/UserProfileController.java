
package com.ilaridamiano.backend.user.profile;

import com.ilaridamiano.backend.user.profile.dto.PublicProfileResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;

    @GetMapping("/{username}")
    public PublicProfileResponse getPublicProfile(
            @PathVariable String username
    ) {
        return profileService.getPublicProfile(username);
    }
}
