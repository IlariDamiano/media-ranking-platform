
package com.ilaridamiano.backend.lists.entity;

import com.ilaridamiano.backend.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_lists")
@Getter
public class MediaList {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 10)
    private ListVisibility visibility;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MediaList() {
    }

    public MediaList(
            AppUser owner,
            String name,
            String description
    ) {
        this.owner = Objects.requireNonNull(
                owner,
                "List owner is required"
        );

        updateDetails(
                name,
                description,
                ListVisibility.PRIVATE
        );
    }

    public void updateDetails(
            String name,
            String description,
            ListVisibility visibility
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "List name must not be blank"
            );
        }

        String cleanName = name.trim();

        if (cleanName.length() > 120) {
            throw new IllegalArgumentException(
                    "List name must not exceed 120 characters"
            );
        }

        if (description != null && description.length() > 2000) {
            throw new IllegalArgumentException(
                    "Description must not exceed 2000 characters"
            );
        }

        this.name = cleanName;
        this.description = description;
        this.visibility = Objects.requireNonNull(
                visibility,
                "Visibility is required"
        );

    }

    public void markModified() {
        this.updatedAt = Instant.now();
    }
}
