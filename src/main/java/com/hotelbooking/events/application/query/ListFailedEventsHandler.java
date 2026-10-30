package com.hotelbooking.events.application.query;

import com.hotelbooking.events.application.model.FailedEvent;
import com.hotelbooking.events.application.port.FailedEventQueries;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListFailedEventsHandler {
    public record Input(UUID hotel) {
    }

    private final FailedEventQueries repository;

    public List<FailedEvent> handle(Input input) {
        return repository.failed(input.hotel());
    }
}
