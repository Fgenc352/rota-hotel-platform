package com.hotelbooking.reservation.infrastructure;

import com.hotelbooking.reservation.application.port.FunnelEvents;
import com.hotelbooking.shared.persistence.JpaQueries;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class FunnelRepository implements FunnelEvents {
    private final JpaQueries queries;

    public void record(UUID hotel, UUID session, String stage) {
        queries.execute("""
            INSERT INTO funnel_event(id,hotel_id,session_id,stage) VALUES (:id,:hotel,:session,:stage)
            ON CONFLICT DO NOTHING
            """,
            Map.of("id", UUID.randomUUID(), "hotel", hotel, "session", session, "stage", stage));
    }
}
