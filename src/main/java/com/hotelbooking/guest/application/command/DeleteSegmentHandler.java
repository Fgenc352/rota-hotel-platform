package com.hotelbooking.guest.application.command;

import com.hotelbooking.guest.application.port.SegmentCommands;
import com.hotelbooking.shared.exception.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteSegmentHandler {
    public record Input(UUID hotel, UUID id) {
    }

    private final SegmentCommands repository;

    @Transactional
    public void handle(Input input) {
        if (!repository.delete(input.hotel(), input.id())) {
            throw BusinessException.missing();
        }
    }
}
