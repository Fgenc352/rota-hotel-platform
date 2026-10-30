package com.hotelbooking.pms.domain;

import java.time.LocalDate;
import java.util.UUID;

public record RoomAssignment(UUID reservationId, UUID roomId, String code, LocalDate checkIn, LocalDate checkOut,
    String status) {
}
