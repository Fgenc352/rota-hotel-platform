package com.hotelbooking.guest.infrastructure;

import com.hotelbooking.guest.application.model.Segment;
import com.hotelbooking.guest.application.port.SegmentQueries;
import com.hotelbooking.guest.infrastructure.jpa.GuestSegmentJpaRepository;
import com.hotelbooking.guest.infrastructure.mapper.SegmentEntityMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SegmentQueryRepository implements SegmentQueries {
    private final GuestSegmentJpaRepository segments;
    private final SegmentEntityMapper mapper;

    public List<Segment> segments(UUID hotel) {
        return segments.findByHotelIdOrderByNameAscIdAsc(hotel).stream().map(mapper::toModel).toList();
    }
    public Optional<Segment> find(UUID hotel, UUID id) {
        return segments.findByIdAndHotelId(id, hotel).map(mapper::toModel);
    }
}
