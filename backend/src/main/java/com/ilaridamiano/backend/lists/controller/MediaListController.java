
package com.ilaridamiano.backend.lists.controller;

import com.ilaridamiano.backend.lists.dto.*;
import com.ilaridamiano.backend.lists.service.MediaListService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MediaListController {

    private final MediaListService listService;

    @PostMapping("/me/lists")
    @ResponseStatus(HttpStatus.CREATED)
    public MediaListResponse create(
            Authentication authentication,
            @Valid @RequestBody CreateMediaListRequest request
    ) {
        return listService.create(
                authentication.getName(),
                request
        );
    }

    @GetMapping("/me/lists")
    public Page<MediaListResponse> findMine(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be >= 0 and size must be between 1 and 100"
            );
        }

        return listService.findMine(
                authentication.getName(),
                page,
                size
        );
    }

    @GetMapping("/lists/{listId}")
    public MediaListDetailsResponse findVisible(
            @PathVariable UUID listId,
            Authentication authentication
    ) {
        return listService.findVisible(
                listId,
                authentication
        );
    }

    @PutMapping("/me/lists/{listId}")
    public MediaListResponse update(
            Authentication authentication,
            @PathVariable UUID listId,
            @Valid @RequestBody UpdateMediaListRequest request
    ) {
        return listService.update(
                authentication.getName(),
                listId,
                request
        );
    }

    @DeleteMapping("/me/lists/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            Authentication authentication,
            @PathVariable UUID listId
    ) {
        listService.delete(
                authentication.getName(),
                listId
        );
    }

    @PostMapping("/me/lists/{listId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public MediaListItemResponse addItem(
            Authentication authentication,
            @PathVariable UUID listId,
            @Valid @RequestBody AddMediaListItemRequest request
    ) {
        return listService.addItem(
                authentication.getName(),
                listId,
                request
        );
    }

    @DeleteMapping("/me/lists/{listId}/items/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(
            Authentication authentication,
            @PathVariable UUID listId,
            @PathVariable UUID mediaId
    ) {
        listService.removeItem(
                authentication.getName(),
                listId,
                mediaId
        );
    }

    @PutMapping("/me/lists/{listId}/order")
    public List<MediaListItemResponse> reorder(
            Authentication authentication,
            @PathVariable UUID listId,
            @Valid @RequestBody ReorderMediaListRequest request
    ) {
        return listService.reorder(
                authentication.getName(),
                listId,
                request
        );
    }
}
