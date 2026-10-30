package com.hotelbooking.guest.application.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GuestReservation(
    UUID hotelId,
    UUID reservationId,
    UUID guestId,
    String code,
    LocalDate checkIn,
    LocalDate checkOut,
    String status,
    BigDecimal total,
    int version,
    Instant checkedOutAt
    ) {
}
