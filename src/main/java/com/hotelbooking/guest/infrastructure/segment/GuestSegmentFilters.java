package com.hotelbooking.guest.infrastructure.segment;

import com.hotelbooking.shared.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GuestSegmentFilters {
    private final List<SegmentFilter> filters;

    public String condition(UUID hotel, String segment, Map<String, Object> parameters) {
        var matches = filters.stream().filter(filter -> filter.supports(segment)).toList();
        if (matches.isEmpty()) {
            throw BusinessException.invalid("Misafir segmenti geçersiz.");
        }
        if (matches.size() != 1) {
            throw new IllegalStateException("Multiple segment filters support the same key");
        }
        return matches.getFirst().condition(hotel, segment, parameters);
    }
}
