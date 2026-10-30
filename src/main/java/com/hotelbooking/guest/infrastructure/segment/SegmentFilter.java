package com.hotelbooking.guest.infrastructure.segment;

import java.util.Map;
import java.util.UUID;

/** SQL adapter extension point; adding a filter does not change GuestQueryRepository. */
public interface SegmentFilter {
    boolean supports(String segment);
    String condition(UUID hotel, String segment, Map<String, Object> parameters);
}
