package com.hotelbooking.analytics.application.port;

import com.hotelbooking.analytics.application.model.Analytics;
import java.util.UUID;

public interface AnalyticsQueries {
    Analytics analytics(UUID hotel);
}
