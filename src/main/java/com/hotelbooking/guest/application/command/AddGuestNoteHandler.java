package com.hotelbooking.guest.application.command;

import com.hotelbooking.guest.application.model.CreatedId;
import com.hotelbooking.guest.application.port.GuestCommands;
import com.hotelbooking.shared.exception.BusinessException;
import com.hotelbooking.shared.logging.TransactionLog;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddGuestNoteHandler {
    public record Input(UUID hotel, UUID id, String username, String body) {
    }

    private final GuestCommands repository;

    @Transactional
    public CreatedId handle(Input input) {
        if (!repository.exists(input.hotel(), input.id())) {
            throw BusinessException.missing();
        }
        UUID id = UUID.randomUUID();
        repository.insertNote(id, input.hotel(), input.id(), input.username(), input.body().strip());
        TransactionLog.afterCommit(() -> log.info("event=guest.note_added hotelId={} guestId={}", input.hotel(),
            input.id()));
        return new CreatedId(id);
    }
}
