package com.hotelbooking.guest.infrastructure;

import com.hotelbooking.guest.application.port.SegmentCommands;
import com.hotelbooking.guest.infrastructure.jpa.GuestSegmentJpaRepository;
import com.hotelbooking.shared.persistence.JpaQueries;
import java.util.HashMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class SegmentCommandRepository implements SegmentCommands {
    private final JpaQueries queries;
    private final GuestSegmentJpaRepository segments;

    public boolean insert(UUID id, UUID hotel, String name, int minStays, int months,
        boolean marketingRequired) {
        var parameters = new HashMap<String, Object>();
        parameters.put("id", id);
        parameters.put("hotel", hotel);
        parameters.put("name", name);
        parameters.put("min", minStays);
        parameters.put("months", months);
        parameters.put("marketing", marketingRequired);
        return queries.execute("""
            INSERT INTO guest_segment(id,hotel_id,name,min_stays,months,marketing_required)
            VALUES (:id,:hotel,:name,:min,:months,:marketing) ON CONFLICT(hotel_id,name) DO NOTHING
            """,
            parameters) > 0;
    }
    public boolean delete(UUID hotel, UUID id) {
        var existing = segments.findByIdAndHotelId(id, hotel);
        existing.ifPresent(segments::delete);
        return existing.isPresent();
    }
}
