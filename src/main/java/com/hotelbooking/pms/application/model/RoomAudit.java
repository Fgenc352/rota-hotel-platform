package com.hotelbooking.pms.application.model;

import java.time.Instant;
import java.util.UUID;

public record RoomAudit(UUID id, UUID roomId, UUID reservationId, String roomNumber, String action,
    String actor, String beforeState, String afterState, Instant createdAt) {
}
