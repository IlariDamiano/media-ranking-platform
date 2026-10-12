
package com.ilaridamiano.backend.library.entity;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "user_media_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_media_entries_user_media",
                columnNames = {"user_id", "media_id"}
        )
)
@Getter
public class UserMediaEntry {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MediaProgressStatus status;

    @Column(name = "rating", precision = 3, scale = 1)
    private BigDecimal rating;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserMediaEntry() {
    }

    public UserMediaEntry(
            AppUser user,
            Media media,
            MediaProgressStatus status,
            BigDecimal rating
    ) {
        this.user = Objects.requireNonNull(user);
        this.media = Objects.requireNonNull(media);

        update(status, rating);
    }

    public void update(
            MediaProgressStatus status,
            BigDecimal rating
    ) {
        this.status = Objects.requireNonNull(
                status,
                "Status is required"
        );

        validateRating(rating);

        this.rating = rating;
    }

    private static void validateRating(BigDecimal rating) {

        if (rating == null) {
            return;
        }

        if (rating.compareTo(new BigDecimal("1.0")) < 0
                || rating.compareTo(new BigDecimal("10.0")) > 0) {
            throw new IllegalArgumentException(
                    "Rating must be between 1.0 and 10.0"
            );
        }

        if (rating.scale() > 1) {
            throw new IllegalArgumentException(
                    "Rating must have at most one decimal place"
            );
        }
    }
}
