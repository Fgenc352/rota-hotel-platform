package com.hotelbooking.pms.application.model;

import com.hotelbooking.pms.domain.ServiceProgress;
import java.time.Instant;
import java.util.UUID;

public record ServiceTaskAudit(UUID id, String code, ServiceProgress previousState, ServiceProgress nextState,
    String actor, Instant createdAt) {
}
