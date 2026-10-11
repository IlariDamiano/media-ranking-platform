package com.ilaridamiano.backend.catalog.controller;

import com.ilaridamiano.backend.catalog.dto.MediaSummaryResponse;
import com.ilaridamiano.backend.catalog.service.MediaService;
import org.springframework.web.bind.annotation.*;
import com.ilaridamiano.backend.integration.tmdb.service.TmdbImportService;
import com.ilaridamiano.backend.catalog.entity.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.ilaridamiano.backend.catalog.enums.MediaType;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaService mediaService;
    private final TmdbImportService tmdbImportService;

    public MediaController(
            MediaService mediaService,
            TmdbImportService tmdbImportService
    ) {
        this.mediaService = mediaService;
        this.tmdbImportService = tmdbImportService;
    }

    @GetMapping
    public Page<MediaSummaryResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(required = false) MediaType mediaType
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be >= 0 and size must be between 1 and 100"
            );
        }

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by("title").ascending()
                        .and(Sort.by("id").ascending())
        );

        return mediaService.findAllPaginated(pageable, mediaType);
    }

    @PostMapping("/import/tmdb/movies/{tmdbId}")
    public MediaSummaryResponse importTmdbMovie(
            @PathVariable Long tmdbId
    ) {
        Media imported = tmdbImportService.importMovie(tmdbId);

        return new MediaSummaryResponse(
                imported.getId(),
                imported.getTitle(),
                imported.getMediaType()
        );
    }

    @GetMapping("/search")
    public List<MediaSummaryResponse> searchMedia(
            @RequestParam String query
    ) {
        return mediaService.searchByTitle(query);
    }

}