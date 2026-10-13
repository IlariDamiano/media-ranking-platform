
package com.ilaridamiano.backend.user.profile;

import com.ilaridamiano.backend.user.profile.dto.MyProfileResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ilaridamiano.backend.user.profile.dto.UpdateMyProfileRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class MyProfileController {

    private final UserProfileService profileService;

    @GetMapping("/me")
    public MyProfileResponse getMyProfile(
            Authentication authentication
    ) {
        return profileService.getMyProfile(authentication.getName());
    }

    @PatchMapping("/me")
    public MyProfileResponse updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateMyProfileRequest request
    ) {
        return profileService.updateMyProfile(
                authentication.getName(),
                request
        );
    }

}
