package com.ilaridamiano.backend.catalog.entity;

import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Table(name = "media_external_ids")
public class MediaExternalId {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_media_type", nullable = false, length = 30)
    private String providerMediaType;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @Setter
    @Column(name = "last_sync_attempt_at")
    private Instant lastSyncAttemptAt;

    @Setter
    @Column(name = "next_sync_attempt_at")
    private Instant nextSyncAttemptAt;

    @Setter
    @Column(name = "sync_failure_count", nullable = false)
    private int syncFailureCount;

    @Setter
    @Column(name = "sync_disabled", nullable = false)
    private boolean syncDisabled;

    protected MediaExternalId() {
    }

    public MediaExternalId(
            Media media,
            String provider,
            String providerMediaType,
            String externalId
    ) {
        this.media = java.util.Objects.requireNonNull(media);
        this.provider = java.util.Objects.requireNonNull(provider);
        this.providerMediaType = java.util.Objects.requireNonNull(providerMediaType);
        this.externalId = java.util.Objects.requireNonNull(externalId);
    }

}
