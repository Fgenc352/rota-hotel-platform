package com.hotelbooking.guest.infrastructure.segment;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AllGuestsFilter implements SegmentFilter {
    @Override
    public boolean supports(String segment) {
        return "ALL".equals(segment);
    }

    @Override
    public String condition(UUID hotel, String segment, Map<String, Object> parameters) {
        return "";
    }
}
