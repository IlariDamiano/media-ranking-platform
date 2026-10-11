package com.ilaridamiano.backend.catalog.entity;

import com.ilaridamiano.backend.catalog.enums.MediaType;
import com.ilaridamiano.backend.catalog.enums.ReleaseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "media")
@Getter
public class Media {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    protected Media() {
    }

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 30)
    private MediaType mediaType;

    @Setter
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Setter
    @Column(name = "release_date")
    private LocalDate releaseDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "format", length = 30)
    private String format;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_status", length = 30)
    private ReleaseStatus releaseStatus;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "release_year")
    private Short releaseYear;

    @Column(name = "original_language", length = 15)
    private String originalLanguage;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "origin_country", length = 2)
    private String originCountry;

    @Column(name = "is_adult")
    private Boolean adult;

    @Setter
    @Column(name = "popularity_score")
    private Double popularityScore;

    @Setter
    @Column(name = "external_rating")
    private Double externalRating;

    @Setter
    @Column(name = "external_rating_count")
    private Integer externalRatingCount;

    @Setter
    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    public Media(String title, MediaType mediaType) {
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.mediaType = Objects.requireNonNull(mediaType, "Media type cannot be null");
    }

}