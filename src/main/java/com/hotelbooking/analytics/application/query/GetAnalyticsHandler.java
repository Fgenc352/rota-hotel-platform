package com.hotelbooking.analytics.application.query;

import com.hotelbooking.analytics.application.model.Analytics;
import com.hotelbooking.analytics.application.port.AnalyticsQueries;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetAnalyticsHandler {
    public record Input(UUID hotel) {
    }

    private final AnalyticsQueries repository;

    public Analytics handle(Input input) {
        return repository.analytics(input.hotel());
    }
}
