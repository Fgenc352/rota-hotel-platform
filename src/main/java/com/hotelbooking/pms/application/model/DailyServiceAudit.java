package com.hotelbooking.pms.application.model;

import java.time.Instant;
import java.util.UUID;

public record DailyServiceAudit(UUID id, String actor, String beforeState, String afterState, Instant createdAt) {
}
