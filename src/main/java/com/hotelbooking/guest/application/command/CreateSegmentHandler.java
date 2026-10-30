package com.hotelbooking.guest.application.command;

import com.hotelbooking.guest.application.model.CreatedId;
import com.hotelbooking.guest.application.port.SegmentCommands;
import com.hotelbooking.shared.exception.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateSegmentHandler {
    public record Input(
        UUID hotel,
        String name,
        int minStays,
        int months,
        boolean marketingRequired
        ) {
    }

    private final SegmentCommands repository;

    @Transactional
    public CreatedId handle(Input input) {
        UUID id = UUID.randomUUID();
        if (!repository.insert(id, input.hotel(), input.name().strip(), input.minStays(), input.months(),
            input.marketingRequired())) {
            throw BusinessException.conflict("SEGMENT_EXISTS", "Bu adla bir segment zaten var.");
        }
        return new CreatedId(id);
    }
}
