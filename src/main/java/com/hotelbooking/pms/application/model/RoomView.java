package com.hotelbooking.pms.application.model;

import com.hotelbooking.pms.domain.Housekeeping;
import java.util.UUID;

public record RoomView(UUID id, UUID roomTypeId, String roomTypeName, String number, Housekeeping housekeeping,
    UUID occupantId, String occupantCode, long upcomingAssignments) {
}
