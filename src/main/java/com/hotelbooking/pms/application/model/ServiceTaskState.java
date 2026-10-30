package com.hotelbooking.pms.application.model;

import com.hotelbooking.pms.domain.ServiceProgress;
import java.time.Instant;

public record ServiceTaskState(String code, ServiceProgress progress, String actor, Instant updatedAt) {
}
