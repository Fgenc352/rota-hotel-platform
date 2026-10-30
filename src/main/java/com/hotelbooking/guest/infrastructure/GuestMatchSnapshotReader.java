package com.hotelbooking.guest.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.hotelbooking.shared.json.Json;
import com.hotelbooking.shared.persistence.JpaQueries;
import com.hotelbooking.shared.persistence.TupleValues;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GuestMatchSnapshotReader {
    private static final String SNAPSHOT = """
        SELECT jsonb_build_object(
            'profile', to_jsonb(g),
            'contacts', coalesce((SELECT jsonb_agg(to_jsonb(i) ORDER BY i.email,i.phone)
                FROM guest_identifier i WHERE i.hotel_id=g.hotel_id AND i.guest_id=g.id), '[]'::jsonb),
            'reservations', coalesce((SELECT jsonb_agg(to_jsonb(r) ORDER BY r.reservation_id)
                FROM crm_reservation r WHERE r.hotel_id=g.hotel_id AND r.guest_id=g.id), '[]'::jsonb),
            'notes', coalesce((SELECT jsonb_agg(to_jsonb(n) ORDER BY n.id)
                FROM guest_note n WHERE n.hotel_id=g.hotel_id AND n.guest_id=g.id), '[]'::jsonb),
            'tags', coalesce((SELECT jsonb_agg(to_jsonb(t) ORDER BY t.tag)
                FROM guest_tag t WHERE t.hotel_id=g.hotel_id AND t.guest_id=g.id), '[]'::jsonb)
        )::text AS snapshot
        FROM guest g WHERE g.hotel_id=:hotel AND g.id=:guest
        """;
    private final JpaQueries queries;
    private final Json json;

    public String capture(UUID hotel, UUID source, UUID target) {
        return json.write(new Snapshot(1, profile(hotel, source), profile(hotel, target)));
    }

    private JsonNode profile(UUID hotel, UUID guest) {
        var rows = queries.tuples(SNAPSHOT, Map.of("hotel", hotel, "guest", guest));
        if (rows.size() != 1) {
            throw new IllegalStateException("Missing guest while recording merge snapshot");
        }
        return json.read(TupleValues.text(rows.getFirst(), "snapshot"), JsonNode.class);
    }

    private record Snapshot(int schemaVersion, JsonNode source, JsonNode target) {
    }
}
