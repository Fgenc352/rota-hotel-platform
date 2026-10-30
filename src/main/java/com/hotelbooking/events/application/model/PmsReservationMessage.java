package com.hotelbooking.events.application.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PmsReservationMessage(UUID eventId, UUID hotelId, UUID reservationId, String code,
    LocalDate checkIn, LocalDate checkOut, String status, int version, Instant checkedOutAt) {
}
