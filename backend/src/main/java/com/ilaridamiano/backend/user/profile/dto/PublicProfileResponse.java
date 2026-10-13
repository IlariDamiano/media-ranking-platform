
package com.ilaridamiano.backend.user.profile.dto;

import java.time.Instant;

public record PublicProfileResponse(
        String username,
        String bio,
        Instant joinedAt
) {
}
