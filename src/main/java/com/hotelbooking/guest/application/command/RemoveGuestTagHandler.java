package com.hotelbooking.guest.application.command;

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
public class RemoveGuestTagHandler {
    public record Input(UUID hotel, UUID id, String tag) {
    }

    private final GuestCommands repository;

    @Transactional
    public void handle(Input input) {
        if (!repository.exists(input.hotel(), input.id())) {
            throw BusinessException.missing();
        }
        repository.untag(input.hotel(), input.id(), input.tag());
        TransactionLog.afterCommit(() -> log.info("event=guest.tag_removed hotelId={} guestId={}", input.hotel(),
            input.id()));
    }
}
