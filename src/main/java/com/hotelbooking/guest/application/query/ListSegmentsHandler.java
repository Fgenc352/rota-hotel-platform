package com.hotelbooking.guest.application.query;

import com.hotelbooking.guest.application.model.Segment;
import com.hotelbooking.guest.application.port.SegmentQueries;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListSegmentsHandler {
    public record Input(UUID hotel) {
    }

    private final SegmentQueries repository;

    public List<Segment> handle(Input input) {
        return repository.segments(input.hotel());
    }
}
