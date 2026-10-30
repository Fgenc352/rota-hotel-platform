package com.hotelbooking.guest.infrastructure.segment;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReengageGuestsFilter implements SegmentFilter {
    @Override
    public boolean supports(String segment) {
        return "REENGAGE".equals(segment);
    }

    @Override
    public String condition(UUID hotel, String segment, Map<String, Object> parameters) {
        return """
            AND g.marketing_allowed
            AND EXISTS (SELECT 1 FROM crm_reservation r
                        WHERE r.hotel_id=g.hotel_id AND r.guest_id=g.id AND r.status='CHECKED_OUT'
                        AND %s = %s - 30)
            """.formatted(SegmentSql.CHECKOUT,
            SegmentSql.TODAY);
    }
}
