package com.hotelbooking.contracts.reservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationSnapshot(
    UUID id,
    UUID hotelId,
    String code,
    LocalDate checkIn,
    LocalDate checkOut,
    String firstName,
    String lastName,
    String email,
    String phone,
    boolean marketingAllowed,
    Instant consentAt,
    String status,
    BigDecimal total,
    int version,
    Instant checkedOutAt
    ) {
}
