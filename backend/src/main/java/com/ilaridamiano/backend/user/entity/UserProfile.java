
package com.ilaridamiano.backend.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Getter
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "avatar_key", length = 80)
    private String avatarKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 10)
    private ProfileVisibility visibility = ProfileVisibility.PUBLIC;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfile() {
    }

    public UserProfile(AppUser user) {
        this.user = Objects.requireNonNull(user);
        this.visibility = ProfileVisibility.PUBLIC;
    }

    public void updateProfile(
            String bio,
            ProfileVisibility visibility
    ) {
        this.bio = bio;
        this.visibility = Objects.requireNonNull(visibility);
    }

}
