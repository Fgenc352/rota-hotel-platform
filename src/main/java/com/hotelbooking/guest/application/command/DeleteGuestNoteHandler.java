package com.hotelbooking.guest.application.command;

import com.hotelbooking.guest.application.port.GuestCommands;
import com.hotelbooking.guest.domain.GuestNoteDeletionPolicy;
import com.hotelbooking.shared.exception.BusinessException;
import com.hotelbooking.shared.logging.TransactionLog;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteGuestNoteHandler {
    public record Input(UUID hotel, UUID guest, UUID note, String actor) {
    }

    private final GuestCommands repository;

    @Transactional
    public void handle(Input input) {
        var note = repository.lockNote(input.hotel(), input.guest(), input.note())
            .orElseThrow(BusinessException::missing);
        GuestNoteDeletionPolicy.validate(note.author(), note.createdAt(), input.actor(), Instant.now());
        repository.deleteNote(input.hotel(), input.guest(), input.note());
        TransactionLog.afterCommit(() -> log.info("event=guest.note_deleted hotelId={} guestId={} noteId={} actor={}",
            input.hotel(), input.guest(), input.note(), input.actor()));
    }
}
