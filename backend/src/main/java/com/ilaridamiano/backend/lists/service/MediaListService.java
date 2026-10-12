
package com.ilaridamiano.backend.lists.service;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.repository.MediaRepository;
import com.ilaridamiano.backend.lists.dto.*;
import com.ilaridamiano.backend.lists.entity.*;
import com.ilaridamiano.backend.lists.repository.*;
import com.ilaridamiano.backend.user.auth.AuthService;
import com.ilaridamiano.backend.user.entity.AppUser;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MediaListService {

    private final MediaListRepository listRepository;
    private final MediaListItemRepository itemRepository;
    private final MediaRepository mediaRepository;
    private final AuthService authService;

    @Transactional
    public MediaListResponse create(
            String email,
            CreateMediaListRequest request
    ) {
        AppUser owner = authService.getCurrentUser(email);

        MediaList list = new MediaList(
                owner,
                request.name(),
                request.description()
        );

        return MediaListResponse.from(
                listRepository.saveAndFlush(list)
        );
    }

    @Transactional(readOnly = true)
    public Page<MediaListResponse> findMine(
            String email,
            int page,
            int size
    ) {
        UUID ownerId = authService.getCurrentUser(email).getId();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("updatedAt"),
                        Sort.Order.desc("id")
                )
        );

        return listRepository.findByOwner_Id(
                ownerId,
                pageable
        ).map(MediaListResponse::from);
    }

    @Transactional(readOnly = true)
    public MediaListDetailsResponse findVisible(
            UUID listId,
            Authentication authentication
    ) {
        MediaList list = listRepository.findById(listId)
                .orElseThrow(this::listNotFound);

        if (list.getVisibility() == ListVisibility.PRIVATE) {

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
                throw listNotFound();
            }

            UUID currentUserId = authService
                    .getCurrentUser(authentication.getName())
                    .getId();

            if (!list.getOwner().getId().equals(currentUserId)) {
                throw listNotFound();
            }
        }

        List<MediaListItemResponse> items = itemRepository
                .findByMediaList_IdOrderByPositionAscIdAsc(listId)
                .stream()
                .map(MediaListItemResponse::from)
                .toList();

        return new MediaListDetailsResponse(
                MediaListResponse.from(list),
                items
        );
    }

    @Transactional
    public MediaListResponse update(
            String email,
            UUID listId,
            UpdateMediaListRequest request
    ) {
        MediaList list = getOwnedForUpdate(email, listId);

        list.updateDetails(
                request.name(),
                request.description(),
                request.visibility()
        );

        return MediaListResponse.from(
                listRepository.saveAndFlush(list)
        );
    }

    @Transactional
    public void delete(String email, UUID listId) {
        MediaList list = getOwnedForUpdate(email, listId);
        listRepository.delete(list);
    }

    @Transactional
    public MediaListItemResponse addItem(
            String email,
            UUID listId,
            AddMediaListItemRequest request
    ) {
        MediaList list = getOwnedForUpdate(email, listId);
        UUID mediaId = request.mediaId();

        if (itemRepository.existsByMediaList_IdAndMedia_Id(
                listId, mediaId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Media is already in this list"
            );
        }

        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Media not found"
                ));

        Integer lastPosition =
                itemRepository.findMaxPosition(listId);

        if (lastPosition != null
                && lastPosition == Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "List position limit reached"
            );
        }

        int nextPosition = lastPosition == null
                ? 0
                : lastPosition + 1;

        MediaListItem saved = itemRepository.saveAndFlush(
                new MediaListItem(list, media, nextPosition)
        );

        list.markModified();
        listRepository.saveAndFlush(list);

        return MediaListItemResponse.from(saved);
    }

    @Transactional
    public void removeItem(
            String email,
            UUID listId,
            UUID mediaId
    ) {
        MediaList list = getOwnedForUpdate(email, listId);

        MediaListItem item = itemRepository
                .findByMediaList_IdAndMedia_Id(listId, mediaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "List item not found"
                ));

        itemRepository.delete(item);

        list.markModified();
        listRepository.saveAndFlush(list);
    }

    @Transactional
    public List<MediaListItemResponse> reorder(
            String email,
            UUID listId,
            ReorderMediaListRequest request
    ) {
        MediaList list = getOwnedForUpdate(email, listId);

        List<MediaListItem> items = itemRepository
                .findByMediaList_IdOrderByPositionAscIdAsc(listId);

        List<UUID> requestedOrder = request.mediaIds();

        if (requestedOrder.size() != items.size()
                || new HashSet<>(requestedOrder).size()
                != requestedOrder.size()) {
            throw invalidOrder();
        }

        Map<UUID, MediaListItem> itemsByMediaId =
                items.stream().collect(Collectors.toMap(
                        item -> item.getMedia().getId(),
                        Function.identity()
                ));

        if (!itemsByMediaId.keySet().equals(
                new HashSet<>(requestedOrder))) {
            throw invalidOrder();
        }

        List<MediaListItemResponse> result = new ArrayList<>();

        for (int position = 0;
             position < requestedOrder.size();
             position++) {

            MediaListItem item = itemsByMediaId.get(
                    requestedOrder.get(position)
            );

            item.changePosition(position);
            result.add(MediaListItemResponse.from(item));
        }

        itemRepository.flush();

        list.markModified();
        listRepository.saveAndFlush(list);

        return result;
    }

    private MediaList getOwnedForUpdate(
            String email,
            UUID listId
    ) {
        UUID ownerId = authService.getCurrentUser(email).getId();

        return listRepository.findOwnedForUpdate(
                listId,
                ownerId
        ).orElseThrow(this::listNotFound);
    }

    private ResponseStatusException listNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "List not found"
        );
    }

    private ResponseStatusException invalidOrder() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Order must contain every current media ID exactly once"
        );
    }
}
