
package com.ilaridamiano.backend.user.profile.dto;

import com.ilaridamiano.backend.user.entity.ProfileVisibility;

import java.time.Instant;
import java.util.UUID;

public record MyProfileResponse(
        UUID id,
        String username,
        String email,
        String bio,
        ProfileVisibility visibility,
        Instant joinedAt
) {
}
