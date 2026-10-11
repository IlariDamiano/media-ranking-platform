package com.ilaridamiano.backend.catalog.service;
import com.ilaridamiano.backend.catalog.dto.MediaSummaryResponse;
import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.repository.MediaRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import com.ilaridamiano.backend.catalog.enums.MediaType;

@Service
public class MediaService {

    private final MediaRepository mediaRepository;

    public MediaService(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    public List<MediaSummaryResponse> findAll() {
        return mediaRepository.findAll()
                .stream()
                .map(media -> new MediaSummaryResponse(
                        media.getId(),
                        media.getTitle(),
                        media.getMediaType()
                ))
                .toList();
    }


    public Page<MediaSummaryResponse> findAllPaginated(
            Pageable pageable,
            MediaType mediaType
    ) {
        Page<Media> mediaPage;

        if (mediaType == null) {
            mediaPage = mediaRepository.findAll(pageable);
        } else {
            mediaPage = mediaRepository.findByMediaType(
                    mediaType,
                    pageable
            );
        }

        return mediaPage.map(media -> new MediaSummaryResponse(
                media.getId(),
                media.getTitle(),
                media.getMediaType()
        ));
    }

    public List<MediaSummaryResponse> searchByTitle(String query) {

        return mediaRepository.searchByRelevance(query)
                .stream()
                .map(media -> new MediaSummaryResponse(
                        media.getId(),
                        media.getTitle(),
                        media.getMediaType()
                ))
                .toList();
    }

}