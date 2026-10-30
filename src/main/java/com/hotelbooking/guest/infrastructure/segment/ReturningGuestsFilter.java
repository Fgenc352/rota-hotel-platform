package com.hotelbooking.guest.infrastructure.segment;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReturningGuestsFilter implements SegmentFilter {
    @Override
    public boolean supports(String segment) {
        return "RETURNING".equals(segment);
    }

    @Override
    public String condition(UUID hotel, String segment, Map<String, Object> parameters) {
        return """
            AND (SELECT count(*) FROM crm_reservation r
                 WHERE r.hotel_id=g.hotel_id AND r.guest_id=g.id AND r.status='CHECKED_OUT'
                 AND %s >= %s - INTERVAL '12 months') >= 2
            """.formatted(SegmentSql.CHECKOUT,
            SegmentSql.TODAY);
    }
}
