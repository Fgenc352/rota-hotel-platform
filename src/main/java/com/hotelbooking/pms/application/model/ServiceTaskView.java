package com.hotelbooking.pms.application.model;

import com.hotelbooking.pms.domain.ServiceProgress;
import com.hotelbooking.reservation.domain.ServiceLine;
import java.time.Instant;

public record ServiceTaskView(ServiceLine line, ServiceProgress progress, boolean trackable, String actor,
    Instant updatedAt) {
}
