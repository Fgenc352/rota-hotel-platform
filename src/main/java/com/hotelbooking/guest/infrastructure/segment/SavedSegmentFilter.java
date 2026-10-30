package com.hotelbooking.guest.infrastructure.segment;

import com.hotelbooking.guest.application.port.SegmentQueries;
import com.hotelbooking.shared.exception.BusinessException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SavedSegmentFilter implements SegmentFilter {
    private final SegmentQueries segments;

    @Override
    public boolean supports(String segment) {
        if (segment == null) {
            return false;
        }
        try {
            UUID.fromString(segment);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String condition(UUID hotel, String segment, Map<String, Object> parameters) {
        var rule = segments.find(hotel, UUID.fromString(segment)).orElseThrow(BusinessException::missing);
        parameters.put("months", rule.months());
        parameters.put("min", rule.minStays());
        var filter = new StringBuilder("""
            AND (SELECT count(*) FROM crm_reservation r
                 WHERE r.hotel_id=g.hotel_id AND r.guest_id=g.id AND r.status='CHECKED_OUT'
                 AND %s >= %s - make_interval(months => :months)) >= :min
            """.formatted(SegmentSql.CHECKOUT,
            SegmentSql.TODAY));
        if (rule.marketingRequired()) {
            filter.append(" AND g.marketing_allowed");
        }
        return filter.toString();
    }
}
