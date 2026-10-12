
package com.ilaridamiano.backend.library.service;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.repository.MediaRepository;
import com.ilaridamiano.backend.library.dto.LibraryEntryResponse;
import com.ilaridamiano.backend.library.dto.SaveLibraryEntryRequest;
import com.ilaridamiano.backend.library.entity.MediaProgressStatus;
import com.ilaridamiano.backend.library.entity.UserMediaEntry;
import com.ilaridamiano.backend.library.repository.UserMediaEntryRepository;
import com.ilaridamiano.backend.user.auth.AuthService;
import com.ilaridamiano.backend.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserMediaLibraryService {

    private final UserMediaEntryRepository entryRepository;
    private final MediaRepository mediaRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public Page<LibraryEntryResponse> findAll(
            String email,
            MediaProgressStatus status,
            int page,
            int size
    ) {

        UUID userId = authService.getCurrentUser(email).getId();

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("updatedAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<UserMediaEntry> entries;

        if (status == null) {
            entries = entryRepository.findByUser_Id(
                    userId,
                    pageable
            );
        } else {
            entries = entryRepository.findByUser_IdAndStatus(
                    userId,
                    status,
                    pageable
            );
        }

        return entries.map(LibraryEntryResponse::from);
    }

    @Transactional(readOnly = true)
    public LibraryEntryResponse findOne(
            String email,
            UUID mediaId
    ) {

        UUID userId = authService.getCurrentUser(email).getId();

        UserMediaEntry entry = getEntry(userId, mediaId);

        return LibraryEntryResponse.from(entry);
    }

    @Transactional
    public LibraryEntryResponse save(
            String email,
            UUID mediaId,
            SaveLibraryEntryRequest request
    ) {

        AppUser user = authService.getCurrentUser(email);

        Optional<UserMediaEntry> existing =
                entryRepository.findByUser_IdAndMedia_Id(
                        user.getId(),
                        mediaId
                );

        UserMediaEntry entry;

        if (existing.isPresent()) {

            entry = existing.get();

            // An omitted rating preserves the existing value.
            BigDecimal rating = request.rating() != null
                    ? request.rating()
                    : entry.getRating();

            entry.update(request.status(), rating);

        } else {

            Media media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Media not found"
                    ));

            entry = new UserMediaEntry(
                    user,
                    media,
                    request.status(),
                    request.rating()
            );
        }

        UserMediaEntry saved = entryRepository.saveAndFlush(entry);

        return LibraryEntryResponse.from(saved);
    }

    @Transactional
    public void delete(String email, UUID mediaId) {

        UUID userId = authService.getCurrentUser(email).getId();

        UserMediaEntry entry = getEntry(userId, mediaId);

        entryRepository.delete(entry);
    }

    @Transactional
    public void clearRating(String email, UUID mediaId) {

        UUID userId = authService.getCurrentUser(email).getId();

        UserMediaEntry entry = getEntry(userId, mediaId);

        entry.update(entry.getStatus(), null);

        entryRepository.saveAndFlush(entry);
    }

    private UserMediaEntry getEntry(
            UUID userId,
            UUID mediaId
    ) {

        return entryRepository.findByUser_IdAndMedia_Id(
                userId,
                mediaId
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Library entry not found"
        ));
    }
}
