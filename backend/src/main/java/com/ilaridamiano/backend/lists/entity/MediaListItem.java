
package com.ilaridamiano.backend.lists.entity;

import com.ilaridamiano.backend.catalog.entity.Media;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "user_list_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_list_items_list_media",
                columnNames = {"list_id", "media_id"}
        )
)
@Getter
public class MediaListItem {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id", nullable = false)
    private MediaList mediaList;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Column(name = "position", nullable = false)
    private int position;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MediaListItem() {
    }

    public MediaListItem(
            MediaList mediaList,
            Media media,
            int position
    ) {
        this.mediaList = Objects.requireNonNull(mediaList);
        this.media = Objects.requireNonNull(media);

        changePosition(position);
    }

    public void changePosition(int position) {
        if (position < 0) {
            throw new IllegalArgumentException(
                    "Position cannot be negative"
            );
        }

        this.position = position;
    }
}
