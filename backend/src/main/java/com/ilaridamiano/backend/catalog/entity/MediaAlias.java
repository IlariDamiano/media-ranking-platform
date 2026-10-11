
package com.ilaridamiano.backend.catalog.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "media_aliases")
public class MediaAlias {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "language_code", length = 15)
    private String languageCode;

    @Column(name = "alias_type", nullable = false, length = 30)
    private String aliasType;

    protected MediaAlias() {
    }

    public MediaAlias(
            Media media,
            String title,
            String languageCode,
            String aliasType
    ) {
        this.media = Objects.requireNonNull(media);
        this.title = Objects.requireNonNull(title);
        this.languageCode = languageCode;
        this.aliasType = Objects.requireNonNull(aliasType);
    }

    public UUID getId() {
        return id;
    }

    public Media getMedia() {
        return media;
    }

    public String getTitle() {
        return title;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getAliasType() {
        return aliasType;
    }
}
