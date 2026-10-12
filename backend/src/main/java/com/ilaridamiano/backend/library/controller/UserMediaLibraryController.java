
package com.ilaridamiano.backend.library.controller;

import com.ilaridamiano.backend.library.dto.LibraryEntryResponse;
import com.ilaridamiano.backend.library.dto.SaveLibraryEntryRequest;
import com.ilaridamiano.backend.library.entity.MediaProgressStatus;
import com.ilaridamiano.backend.library.service.UserMediaLibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/me/media")
@RequiredArgsConstructor
public class UserMediaLibraryController {

    private final UserMediaLibraryService libraryService;

    @GetMapping
    public Page<LibraryEntryResponse> findAll(
            Authentication authentication,
            @RequestParam(required = false) MediaProgressStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size
    ) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be >= 0 and size must be between 1 and 100"
            );
        }

        return libraryService.findAll(
                authentication.getName(),
                status,
                page,
                size
        );
    }

    @GetMapping("/{mediaId}")
    public LibraryEntryResponse findOne(
            Authentication authentication,
            @PathVariable UUID mediaId
    ) {

        return libraryService.findOne(
                authentication.getName(),
                mediaId
        );
    }

    @PutMapping("/{mediaId}")
    public LibraryEntryResponse save(
            Authentication authentication,
            @PathVariable UUID mediaId,
            @Valid @RequestBody SaveLibraryEntryRequest request
    ) {

        return libraryService.save(
                authentication.getName(),
                mediaId,
                request
        );
    }

    @DeleteMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            Authentication authentication,
            @PathVariable UUID mediaId
    ) {

        libraryService.delete(
                authentication.getName(),
                mediaId
        );
    }

    @DeleteMapping("/{mediaId}/rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearRating(
            Authentication authentication,
            @PathVariable UUID mediaId
    ) {

        libraryService.clearRating(
                authentication.getName(),
                mediaId
        );
    }
}
