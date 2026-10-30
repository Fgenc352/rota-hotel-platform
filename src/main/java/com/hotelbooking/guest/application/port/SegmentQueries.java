package com.hotelbooking.guest.application.port;

import com.hotelbooking.guest.application.model.Segment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SegmentQueries {
    List<Segment> segments(UUID hotel);

    Optional<Segment> find(UUID hotel, UUID id);
}
