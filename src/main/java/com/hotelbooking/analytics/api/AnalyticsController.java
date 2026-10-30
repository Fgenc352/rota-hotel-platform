package com.hotelbooking.analytics.api;

import com.hotelbooking.analytics.application.model.Analytics;
import com.hotelbooking.analytics.application.query.GetAnalyticsHandler;
import com.hotelbooking.identity.application.model.CurrentStaff;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm")
@Validated
@RequiredArgsConstructor
public class AnalyticsController {
    private final GetAnalyticsHandler getAnalytics;

    @GetMapping("/analytics") Analytics analytics(@Parameter(hidden = true) CurrentStaff staff) {
        UUID hotel = staff.hotelId();
        return getAnalytics.handle(new GetAnalyticsHandler.Input(hotel));
    }
}
